#!/bin/bash

# Configuration
SERVER_DIR="run"
MINECRAFT_VERSION="1.21.11"

# Create server directory if it doesn't exist
mkdir -p "$SERVER_DIR/plugins/Atlas"
mkdir -p "$SERVER_DIR/plugins/Nexo/items"
mkdir -p "$SERVER_DIR/plugins/Nexo/recipes/shapeless"
mkdir -p "$SERVER_DIR/plugins/Nexo/pack/assets/atlas/textures/block"

# Build the plugin
echo "Building plugin..."
mvn package -q

if [ $? -ne 0 ]; then
    echo "Build failed!"
    exit 1
fi

# Copy the plugin to the server
cp target/Atlas-*.jar "$SERVER_DIR/plugins/"

# Check for Nexo plugin (accept any nexo*.jar or Nexo*.jar file)
NEXO_JAR=$(find "$SERVER_DIR/plugins" -maxdepth 1 \( -name "nexo*.jar" -o -name "Nexo*.jar" \) 2>/dev/null | head -1)

# Validate the JAR has plugin.yml (not just the API artifact)
if [ -n "$NEXO_JAR" ]; then
    if ! unzip -l "$NEXO_JAR" 2>/dev/null | grep -q "plugin.yml"; then
        echo "Found invalid Nexo JAR (API artifact, not plugin). Removing..."
        rm -f "$NEXO_JAR"
        rm -rf "$SERVER_DIR/plugins/.paper-remapped/nexo-"*
        rm -rf "$SERVER_DIR/plugins/.paper-remapped/Nexo-"*
        NEXO_JAR=""
    fi
fi

if [ -z "$NEXO_JAR" ]; then
    # Clean up any corrupted files
    rm -rf "$SERVER_DIR/plugins/.paper-remapped/nexo-"*
    rm -rf "$SERVER_DIR/plugins/.paper-remapped/Nexo-"*

    echo ""
    echo "=============================================="
    echo "  Nexo plugin not found!"
    echo "=============================================="
    echo ""
    echo "  Download from Polymart:"
    echo "    https://polymart.org/resource/nexo.6901"
    echo ""
    echo "  Or from BuiltByBit:"
    echo "    https://builtbybit.com/resources/nexo.36138/"
    echo ""
    echo "  Then place the JAR in:"
    echo "    $(pwd)/$SERVER_DIR/plugins/"
    echo ""
    echo "  NOTE: The Maven repository only has the API."
    echo "        You need the full plugin from Polymart/BuiltByBit."
    echo "=============================================="
    echo ""
    exit 1
fi

echo "Found Nexo: $NEXO_JAR"

# Pre-copy Atlas Nexo configurations (so they're available on first startup)
echo "Setting up Atlas Nexo configurations..."
cp "src/main/resources/nexo/items/atlas_blocks.yml" "$SERVER_DIR/plugins/Nexo/items/"
cp "src/main/resources/nexo/recipes/shapeless/atlas_recipes.yml" "$SERVER_DIR/plugins/Nexo/recipes/shapeless/"
cp "src/main/resources/nexo/pack/assets/atlas/textures/block/test_block.png" "$SERVER_DIR/plugins/Nexo/pack/assets/atlas/textures/block/"

# Create/update the Atlas plugin config (resource pack disabled - Nexo handles it)
cat > "$SERVER_DIR/plugins/Atlas/config.yml" << EOF
# Atlas Plugin Configuration (auto-generated for testing)
# Resource pack is disabled because Nexo handles resource pack generation and hosting

resource-pack:
  enabled: false
  url: ""
  hash: ""
  required: false
  prompt: ""
EOF

echo "Atlas config created (resource pack disabled - Nexo handles it)"

# Download Paper if not present or update to latest build
cd "$SERVER_DIR"

echo "Fetching latest Paper build info..."
BUILD_INFO=$(curl -s "https://api.papermc.io/v2/projects/paper/versions/$MINECRAFT_VERSION/builds" | grep -o '"build":[0-9]*' | tail -1 | grep -o '[0-9]*')

if [ -z "$BUILD_INFO" ]; then
    echo "Failed to fetch build info!"
    exit 1
fi

PAPER_JAR="paper-$MINECRAFT_VERSION-$BUILD_INFO.jar"

if [ ! -f "$PAPER_JAR" ]; then
    echo "Downloading Paper build $BUILD_INFO..."
    # Remove old Paper jars
    rm -f paper-*.jar
    curl -o "$PAPER_JAR" "https://api.papermc.io/v2/projects/paper/versions/$MINECRAFT_VERSION/builds/$BUILD_INFO/downloads/$PAPER_JAR"
fi

# Accept EULA
echo "eula=true" > eula.txt

# Configure Paper for Nexo (disable noteblock/tripwire/chorus updates for performance)
mkdir -p "config"
if [ ! -f "config/paper-global.yml" ]; then
    cat > "config/paper-global.yml" << EOF
# Paper Global Configuration
# Optimized for Nexo custom blocks

block-updates:
  disable-noteblock-updates: true
  disable-tripwire-updates: true
  disable-chorus-plant-updates: true
EOF
    echo "Paper config created with Nexo optimizations"
else
    # Check if optimizations are already present, if not warn the user
    if ! grep -q "disable-noteblock-updates: true" "config/paper-global.yml"; then
        echo ""
        echo "WARNING: Please add these settings to config/paper-global.yml for Nexo:"
        echo "  block-updates:"
        echo "    disable-noteblock-updates: true"
        echo "    disable-tripwire-updates: true"
        echo "    disable-chorus-plant-updates: true"
        echo ""
    fi
fi

echo ""
echo "=============================================="
echo "  Atlas + Nexo Development Server"
echo "=============================================="
echo "  Nexo will handle resource pack hosting"
echo "  Craft TestBlock: 3x Paper in crafting table"
echo "  Or use /testblock to get one directly"
echo "  Use /nexo reload to reload configurations"
echo "=============================================="
echo ""

# Run the server
echo "Starting Paper server..."
java -Xms1G -Xmx2G -jar "$PAPER_JAR" --nogui

#!/bin/bash

# Configuration
SERVER_DIR="run"
MINECRAFT_VERSION="1.21.11"

# Create server directory if it doesn't exist
mkdir -p "$SERVER_DIR/plugins"
mkdir -p "$SERVER_DIR/plugins/CraftEngine/resources"

# Build the plugin
echo "Building plugin..."
mvn clean install package -q

if [ $? -ne 0 ]; then
    echo "Build failed!"
    exit 1
fi

# Copy the plugin to the server
cp target/Atlas-*.jar "$SERVER_DIR/plugins/"

# Check for CraftEngine plugin
CE_JAR=$(find "$SERVER_DIR/plugins" -maxdepth 1 \( -name "craft-engine*.jar" -o -name "CraftEngine*.jar" \) 2>/dev/null | head -1)

if [ -n "$CE_JAR" ]; then
    if ! unzip -l "$CE_JAR" 2>/dev/null | grep -q "plugin.yml\|paper-plugin.yml"; then
        echo "Found invalid CraftEngine JAR (API artifact, not plugin). Removing..."
        rm -f "$CE_JAR"
        rm -rf "$SERVER_DIR/plugins/.paper-remapped/craft-engine-"*
        rm -rf "$SERVER_DIR/plugins/.paper-remapped/CraftEngine-"*
        CE_JAR=""
    fi
fi

if [ -z "$CE_JAR" ]; then
    rm -rf "$SERVER_DIR/plugins/.paper-remapped/craft-engine-"*
    rm -rf "$SERVER_DIR/plugins/.paper-remapped/CraftEngine-"*

    echo ""
    echo "=============================================="
    echo "  CraftEngine plugin not found!"
    echo "=============================================="
    echo ""
    echo "  Download from Polymart:"
    echo "    https://polymart.org/product/7624/craftengine"
    echo ""
    echo "  Or from BuiltByBit:"
    echo "    https://builtbybit.com/resources/craftengine.82674/"
    echo ""
    echo "  Or from Modrinth:"
    echo "    https://modrinth.com/plugin/craftengine"
    echo ""
    echo "  Then place the JAR in:"
    echo "    $(pwd)/$SERVER_DIR/plugins/"
    echo ""
    echo "  NOTE: The Maven repository only has the API."
    echo "        You need the full plugin JAR."
    echo "=============================================="
    echo ""
    exit 1
fi

echo "Found CraftEngine: $CE_JAR"

# Create/update the Atlas plugin config (resource pack disabled - CraftEngine handles it)
cat > "$SERVER_DIR/plugins/Atlas/config.yml" << EOF
# Atlas Plugin Configuration (auto-generated for testing)
# Resource pack is disabled because CraftEngine handles resource pack generation and hosting

logging: false

resource-pack:
  enabled: false
  url: ""
  hash: ""
  required: false
  prompt: ""
EOF

echo "Atlas config created (resource pack disabled - CraftEngine handles it)"

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

# Configure Paper for CraftEngine (disable noteblock/tripwire/chorus updates for performance)
mkdir -p "config"
if [ ! -f "config/paper-global.yml" ]; then
    cat > "config/paper-global.yml" << EOF
# Paper Global Configuration

block-updates:
  disable-noteblock-updates: true
  disable-tripwire-updates: true
  disable-chorus-plant-updates: true
EOF
    echo "Paper config created with CraftEngine optimizations"
else
    # Check if optimizations are already present, if not warn the user
    if ! grep -q "disable-noteblock-updates: true" "config/paper-global.yml"; then
        echo ""
        echo "WARNING: Please add these settings to config/paper-global.yml for CraftEngine:"
        echo "  block-updates:"
        echo "    disable-noteblock-updates: true"
        echo "    disable-tripwire-updates: true"
        echo "    disable-chorus-plant-updates: true"
        echo ""
    fi
fi

# Run the server
echo "Starting Paper server..."
java -Xms1G -Xmx2G -jar "$PAPER_JAR" --nogui

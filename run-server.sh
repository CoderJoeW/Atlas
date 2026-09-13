#!/bin/bash

# Configuration
SERVER_DIR="run"
MINECRAFT_VERSION="26.2"

remove_ce_remapped() {
    rm -rf "$SERVER_DIR/plugins/.paper-remapped/craft-engine-"*
    rm -rf "$SERVER_DIR/plugins/.paper-remapped/CraftEngine-"*
}

build_plugin() {
    echo "Building plugin..."
    mvn clean install package -q

    if [ $? -ne 0 ]; then
        echo "Build failed!"
        exit 1
    fi

    cp target/Atlas-*.jar "$SERVER_DIR/plugins/"
}

setup_craftengine() {
    # Check for CraftEngine plugin
    CE_JAR=$(find "$SERVER_DIR/plugins" -maxdepth 1 \( -name "craft-engine*.jar" -o -name "CraftEngine*.jar" \) 2>/dev/null | head -1)

    if [ -n "$CE_JAR" ]; then
        if ! unzip -l "$CE_JAR" 2>/dev/null | grep -q "plugin.yml\|paper-plugin.yml"; then
            echo "Found invalid CraftEngine JAR (API artifact, not plugin). Removing..."
            rm -f "$CE_JAR"
            remove_ce_remapped
            CE_JAR=""
        fi
    fi

    if [ -z "$CE_JAR" ]; then
        remove_ce_remapped

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

    # CraftEngine only extracts a shipped resource pack when it is not already on disk, so
    # upgrading the jar leaves the previously extracted packs untouched and never writes packs
    # the new jar added. A missing 'internal' pack empties the visual block state pool
    # (mappings.yml), and every custom block then fails to allocate a state at load time.
    # Delete the shipped packs whenever the jar changes so CraftEngine re-extracts them.
    # The 'atlas' pack is left alone - the Atlas plugin rewrites it from its own jar on startup.
    CE_RESOURCES="$SERVER_DIR/plugins/CraftEngine/resources"
    CE_MARKER="$SERVER_DIR/plugins/CraftEngine/.ce-version"
    CE_HASH=$(shasum -a 256 "$CE_JAR" | cut -d ' ' -f 1)

    if [ ! -f "$CE_MARKER" ] || [ "$(cat "$CE_MARKER")" != "$CE_HASH" ]; then
        if [ -d "$CE_RESOURCES" ]; then
            echo "CraftEngine jar changed - removing its shipped resource packs for re-extraction..."
            rm -rf "$CE_RESOURCES/internal" "$CE_RESOURCES"/default_*
        fi
        mkdir -p "$(dirname "$CE_MARKER")"
        echo "$CE_HASH" > "$CE_MARKER"
    fi
}

write_atlas_config() {
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
}

fetch_paper() {
    local paper_ua="Atlas-run-server/1.0.0 (j.williamson@gambler-labs.com)"

    echo "Fetching latest Paper build info..."
    local builds_response
    builds_response=$(curl -s -H "User-Agent: $paper_ua" \
        "https://fill.papermc.io/v3/projects/paper/versions/$MINECRAFT_VERSION/builds")

    if [ "$(echo "$builds_response" | jq -r 'type')" != "array" ]; then
        echo "Failed to fetch build info: $(echo "$builds_response" | jq -r '.message // .')"
        exit 1
    fi

    local build_json
    build_json=$(echo "$builds_response" | jq -c '[.[] | select(.channel == "STABLE")] | last')

    if [ -z "$build_json" ] || [ "$build_json" = "null" ]; then
        echo "Failed to fetch build info!"
        exit 1
    fi

    PAPER_JAR=$(echo "$build_json" | jq -r '.downloads["server:default"].name')
    local paper_url
    paper_url=$(echo "$build_json" | jq -r '.downloads["server:default"].url')

    if [ ! -f "$PAPER_JAR" ]; then
        echo "Downloading $PAPER_JAR..."
        # Remove old Paper jars
        rm -f paper-*.jar
        curl -H "User-Agent: $paper_ua" -o "$PAPER_JAR" "$paper_url"
    fi

    # Accept EULA
    echo "eula=true" > eula.txt
}

configure_paper_global() {
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
}

run_server() {
    echo "Starting Paper server..."
    java -Xms1G -Xmx2G -jar "$PAPER_JAR" --nogui
}

main() {
    mkdir -p "$SERVER_DIR/plugins"

    build_plugin
    setup_craftengine
    write_atlas_config

    cd "$SERVER_DIR"

    fetch_paper
    configure_paper_global
    run_server
}

main

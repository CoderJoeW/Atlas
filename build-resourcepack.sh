#!/bin/bash
# Build the Atlas resource pack

cd "$(dirname "$0")/resourcepack"

# Create the zip file
zip -r ../Atlas-ResourcePack.zip . -x "*.DS_Store" -x "*.txt"

cd ..

echo "Resource pack created: Atlas-ResourcePack.zip"

# Calculate and display the SHA-1 hash
if command -v shasum &> /dev/null; then
    HASH=$(shasum -a 1 Atlas-ResourcePack.zip | cut -d ' ' -f 1)
    echo "SHA-1 hash: $HASH"
elif command -v sha1sum &> /dev/null; then
    HASH=$(sha1sum Atlas-ResourcePack.zip | cut -d ' ' -f 1)
    echo "SHA-1 hash: $HASH"
fi

echo ""
echo "For manual installation: place this file in your Minecraft resourcepacks folder"
echo "For server use: configure the URL and hash in plugins/Atlas/config.yml"

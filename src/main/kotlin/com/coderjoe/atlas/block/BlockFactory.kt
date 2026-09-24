package com.coderjoe.atlas.block

import org.bukkit.Location
import org.bukkit.block.BlockFace

open class BlockFactory {
    private val blockConstructors = mutableMapOf<String, (Location, BlockFace) -> AtlasBlock>()

    fun register(
        blockId: String,
        constructor: (Location, BlockFace) -> AtlasBlock,
    ) {
        blockConstructors[blockId] = constructor
    }

    fun create(
        blockId: String,
        location: Location,
        facing: BlockFace = BlockFace.SELF,
    ): AtlasBlock? {
        return blockConstructors[blockId]?.invoke(location, facing)
    }

    fun isRegistered(blockId: String): Boolean {
        return blockConstructors.containsKey(blockId)
    }

    fun getRegisteredBlockIds(): Set<String> {
        return blockConstructors.keys
    }

    fun registerFromDescriptors(descriptors: Collection<BlockDescriptor>) {
        for (desc in descriptors) {
            register(desc.baseBlockId, desc.constructor)
            for (id in desc.additionalBlockIds) {
                register(id, desc.constructor)
            }
        }
    }

    fun clear() {
        blockConstructors.clear()
    }
}

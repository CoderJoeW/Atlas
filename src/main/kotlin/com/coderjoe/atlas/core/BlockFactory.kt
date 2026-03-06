package com.coderjoe.atlas.core

import org.bukkit.Location
import org.bukkit.block.BlockFace

open class BlockFactory<T : AtlasBlock> {
    private val blockConstructors = mutableMapOf<String, (Location, BlockFace) -> T>()

    fun register(blockId: String, constructor: (Location, BlockFace) -> T) {
        blockConstructors[blockId] = constructor
    }

    fun create(blockId: String, location: Location, facing: BlockFace = BlockFace.SELF): T? {
        return blockConstructors[blockId]?.invoke(location, facing)
    }

    fun isRegistered(blockId: String): Boolean {
        return blockConstructors.containsKey(blockId)
    }

    fun getRegisteredBlockIds(): Set<String> {
        return blockConstructors.keys
    }

    @Suppress("UNCHECKED_CAST")
    fun registerFromDescriptors(descriptors: Collection<BlockDescriptor>) {
        for (desc in descriptors) {
            for (id in desc.allRegistrableIds) {
                register(id, desc.constructor as (Location, BlockFace) -> T)
            }
        }
    }

    fun clear() {
        blockConstructors.clear()
    }
}

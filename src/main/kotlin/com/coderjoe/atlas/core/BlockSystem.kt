package com.coderjoe.atlas.core

import org.bukkit.entity.Player

class BlockSystem<T : AtlasBlock>(
    val name: String,
    val registry: BlockRegistry<T>,
    val factory: BlockFactory<T>,
    val descriptors: Map<String, BlockDescriptor>,
    val showDialog: (Player, AtlasBlock) -> Unit,
) {
    fun findDescriptorForBlockId(blockId: String): BlockDescriptor? {
        return descriptors.values.find { blockId in it.allRegistrableIds }
    }

    fun findDescriptorByBaseId(blockId: String): BlockDescriptor? {
        return descriptors[blockId]
    }
}

package com.coderjoe.atlas.block

class BlockSystem(
    val name: String,
    val registry: BlockRegistry,
    val factory: BlockFactory,
    val descriptors: Map<String, BlockDescriptor>,
) {
    fun findDescriptorForBlockId(blockId: String): BlockDescriptor? {
        return descriptors.values.find { blockId == it.baseBlockId || blockId in it.additionalBlockIds }
    }

    fun findDescriptorByBaseId(blockId: String): BlockDescriptor? {
        return descriptors[blockId]
    }
}

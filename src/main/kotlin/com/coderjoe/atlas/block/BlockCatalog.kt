package com.coderjoe.atlas.block

import org.bukkit.Location
import org.bukkit.block.BlockFace

class BlockCatalog(val descriptors: List<BlockDescriptor>) {
    private val byId: Map<String, BlockDescriptor> =
        descriptors
            .flatMap { d -> (listOf(d.baseBlockId) + d.additionalBlockIds).map { id -> id to d } }
            .toMap()

    init {
        val declared = descriptors.sumOf { 1 + it.additionalBlockIds.size }
        require(byId.size == declared) { "Two descriptors claim the same block id" }
    }

    val blockIds: Set<String> get() = byId.keys

    fun find(blockId: String): BlockDescriptor? = byId[blockId]

    fun create(
        blockId: String,
        location: Location,
        facing: BlockFace = BlockFace.SELF,
    ): AtlasBlock? {
        return byId[blockId]?.constructor?.invoke(location, facing)
    }
}

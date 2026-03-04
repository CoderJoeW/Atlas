package com.coderjoe.atlas.fluid

import org.bukkit.Location
import org.bukkit.block.BlockFace

object FluidBlockFactory {
    private val blockConstructors = mutableMapOf<String, (Location, BlockFace) -> FluidBlock>()

    fun register(blockId: String, constructor: (Location, BlockFace) -> FluidBlock) {
        blockConstructors[blockId] = constructor
        println("FluidBlockFactory: Registered block ID '$blockId'")
    }

    fun createFluidBlock(blockId: String, location: Location, facing: BlockFace = BlockFace.SELF): FluidBlock? {
        return blockConstructors[blockId]?.invoke(location, facing)
    }

    fun isRegistered(blockId: String): Boolean {
        return blockConstructors.containsKey(blockId)
    }

    fun getRegisteredBlockIds(): Set<String> {
        return blockConstructors.keys
    }
}

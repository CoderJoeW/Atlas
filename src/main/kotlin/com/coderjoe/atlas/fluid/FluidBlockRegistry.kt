package com.coderjoe.atlas.fluid

import com.coderjoe.atlas.core.BlockRegistry
import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.bukkit.plugin.java.JavaPlugin

class FluidBlockRegistry(plugin: JavaPlugin) : BlockRegistry<FluidBlock>(plugin) {
    companion object {
        var instance: FluidBlockRegistry? = null
            private set
    }

    init {
        instance = this
    }

    fun registerFluidBlock(
        fluidBlock: FluidBlock,
        blockId: String,
    ) = register(fluidBlock, blockId)

    fun unregisterFluidBlock(location: Location): FluidBlock? = unregister(location)

    fun getFluidBlock(location: Location): FluidBlock? = getBlock(location)

    fun getAdjacentFluidBlock(
        location: Location,
        face: BlockFace,
    ): FluidBlock? = getAdjacentBlock(location, face)

    fun getAllFluidBlocksWithIds(): List<Pair<FluidBlock, String>> = getAllBlocksWithIds()
}

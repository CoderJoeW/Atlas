package com.coderjoe.atlas.power

import com.coderjoe.atlas.core.BlockRegistry
import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.bukkit.plugin.java.JavaPlugin

class PowerBlockRegistry(plugin: JavaPlugin) : BlockRegistry<PowerBlock>(plugin) {
    companion object {
        var instance: PowerBlockRegistry? = null
            private set
    }

    init {
        instance = this
    }

    fun registerPowerBlock(
        powerBlock: PowerBlock,
        blockId: String,
    ) = register(powerBlock, blockId)

    fun unregisterPowerBlock(location: Location): PowerBlock? = unregister(location)

    fun getPowerBlock(location: Location): PowerBlock? = getBlock(location)

    fun getAdjacentPowerBlock(
        location: Location,
        face: BlockFace,
    ): PowerBlock? = getAdjacentBlock(location, face)

    fun getAdjacentPowerBlocks(location: Location): List<PowerBlock> = getAdjacentBlocks(location)

    fun getAllPowerBlocksWithIds(): List<Pair<PowerBlock, String>> = getAllBlocksWithIds()

    fun getAllPowerBlocks(): Collection<PowerBlock> = getAllBlocks()
}

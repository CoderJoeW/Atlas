package com.coderjoe.atlas.power

import org.bukkit.Location

data class PowerBlockData(
    val blockId: String,
    val world: String,
    val x: Int,
    val y: Int,
    val z: Int,
    val currentPower: Int
) {
    companion object {
        fun fromPowerBlock(powerBlock: PowerBlock, blockId: String): PowerBlockData {
            val loc = powerBlock.location
            return PowerBlockData(
                blockId = blockId,
                world = loc.world?.name ?: "world",
                x = loc.blockX,
                y = loc.blockY,
                z = loc.blockZ,
                currentPower = powerBlock.currentPower
            )
        }
    }

    fun toLocation(plugin: org.bukkit.plugin.java.JavaPlugin): Location? {
        val world = plugin.server.getWorld(this.world) ?: return null
        return Location(world, x.toDouble(), y.toDouble(), z.toDouble())
    }
}

package com.coderjoe.atlas.power

import com.coderjoe.atlas.utility.block.SmallDrill
import org.bukkit.Location
import org.bukkit.block.BlockFace

data class PowerBlockData(
    val blockId: String,
    val world: String,
    val x: Int,
    val y: Int,
    val z: Int,
    val currentPower: Int,
    val facing: String? = null,
    val enabled: Boolean? = null,
) {
    companion object {
        fun fromPowerBlock(
            powerBlock: PowerBlock,
            blockId: String,
        ): PowerBlockData {
            val loc = powerBlock.location
            val facing = powerBlock.facing.let { if (it == BlockFace.SELF) null else it.name }
            val enabled = if (powerBlock is SmallDrill) powerBlock.enabled else null
            return PowerBlockData(
                blockId = blockId,
                world = loc.world?.name ?: "world",
                x = loc.blockX,
                y = loc.blockY,
                z = loc.blockZ,
                currentPower = powerBlock.currentPower,
                facing = facing,
                enabled = enabled,
            )
        }
    }

    fun toLocation(plugin: org.bukkit.plugin.java.JavaPlugin): Location? {
        val world = plugin.server.getWorld(this.world) ?: return null
        return Location(world, x.toDouble(), y.toDouble(), z.toDouble())
    }

    fun toBlockFace(): BlockFace {
        return if (facing != null) {
            try {
                BlockFace.valueOf(facing)
            } catch (_: Exception) {
                BlockFace.SELF
            }
        } else {
            BlockFace.SELF
        }
    }
}

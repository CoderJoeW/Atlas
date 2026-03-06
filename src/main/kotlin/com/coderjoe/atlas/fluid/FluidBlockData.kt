package com.coderjoe.atlas.fluid

import com.coderjoe.atlas.fluid.block.FluidContainer
import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.bukkit.plugin.java.JavaPlugin

data class FluidBlockData(
    val blockId: String,
    val world: String,
    val x: Int,
    val y: Int,
    val z: Int,
    val fluidType: String,
    val facing: String? = null,
    val storedAmount: Int? = null
) {
    companion object {
        fun fromFluidBlock(fluidBlock: FluidBlock, blockId: String): FluidBlockData {
            val loc = fluidBlock.location
            val facing = fluidBlock.facing.let { if (it == BlockFace.SELF) null else it.name }
            val storedAmount = when (fluidBlock) {
                is FluidContainer -> fluidBlock.storedAmount
                else -> null
            }
            return FluidBlockData(
                blockId = blockId,
                world = loc.world?.name ?: "world",
                x = loc.blockX,
                y = loc.blockY,
                z = loc.blockZ,
                fluidType = fluidBlock.storedFluid.name,
                facing = facing,
                storedAmount = storedAmount
            )
        }
    }

    fun toLocation(plugin: JavaPlugin): Location? {
        val world = plugin.server.getWorld(this.world) ?: return null
        return Location(world, x.toDouble(), y.toDouble(), z.toDouble())
    }

    fun toBlockFace(): BlockFace {
        return if (facing != null) {
            try { BlockFace.valueOf(facing) } catch (_: Exception) { BlockFace.SELF }
        } else {
            BlockFace.SELF
        }
    }

    fun toFluidType(): FluidType {
        return try { FluidType.valueOf(fluidType) } catch (_: Exception) { FluidType.NONE }
    }
}

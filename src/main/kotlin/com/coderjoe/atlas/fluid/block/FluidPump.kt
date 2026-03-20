package com.coderjoe.atlas.fluid.block

import com.coderjoe.atlas.atlasInfo
import com.coderjoe.atlas.coordinates
import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.PlacementType
import com.coderjoe.atlas.fluid.FluidBlock
import com.coderjoe.atlas.fluid.FluidType
import com.coderjoe.atlas.power.PowerBlockRegistry
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Levelled

class FluidPump(location: Location) : FluidBlock(location) {
    enum class PumpStatus {
        IDLE,
        NO_SOURCE,
        NO_POWER,
        EXTRACTING,
    }

    override val updateIntervalTicks: Long = 20L

    var cauldronFace: BlockFace? = null
        private set

    var isPowered: Boolean = false
        private set

    var pumpStatus: PumpStatus = PumpStatus.NO_SOURCE
        private set

    companion object {
        const val BLOCK_ID = "atlas:fluid_pump"
        const val BLOCK_ID_ACTIVE = "atlas:fluid_pump_active"
        const val BLOCK_ID_ACTIVE_LAVA = "atlas:fluid_pump_active_lava"

        val descriptor =
            BlockDescriptor(
                baseBlockId = BLOCK_ID,
                displayName = "Fluid Pump",
                description = "Pump - extracts fluid from adjacent cauldrons or source blocks (1 power/s)",
                placementType = PlacementType.SIMPLE,
                additionalBlockIds = listOf(BLOCK_ID_ACTIVE, BLOCK_ID_ACTIVE_LAVA),
                constructor = { loc, _ -> FluidPump(loc) },
            )
    }

    override val baseBlockId: String = BLOCK_ID

    fun canRemoveFluidFrom(direction: BlockFace): Boolean {
        val cauldron = cauldronFace ?: return false
        return direction == cauldron.oppositeFace && hasFluid()
    }

    override fun canProvideFluid(requestDirection: BlockFace): Boolean = canRemoveFluidFrom(requestDirection)

    override fun getVisualStateBlockId(): String =
        when (storedFluid) {
            FluidType.WATER -> BLOCK_ID_ACTIVE
            FluidType.LAVA -> BLOCK_ID_ACTIVE_LAVA
            FluidType.NONE -> BLOCK_ID
        }

    override fun fluidUpdate() {
        val powerRegistry = PowerBlockRegistry.instance ?: return
        val powerNeighbors = powerRegistry.getAdjacentPowerBlocks(location)
        isPowered = powerNeighbors.any { it.hasPower() }

        if (hasFluid()) {
            pumpStatus = PumpStatus.IDLE
            return
        }

        var foundFace: BlockFace? = null
        var foundBlock: org.bukkit.block.Block? = null
        var fluidType = FluidType.NONE

        for (face in ADJACENT_FACES) {
            val offset = face.direction
            val adjacentBlock =
                location.world?.getBlockAt(
                    location.blockX + offset.blockX,
                    location.blockY + offset.blockY,
                    location.blockZ + offset.blockZ,
                ) ?: continue

            val type =
                when (adjacentBlock.type) {
                    Material.WATER_CAULDRON -> FluidType.WATER
                    Material.LAVA_CAULDRON -> FluidType.LAVA
                    Material.WATER -> {
                        val levelData = adjacentBlock.blockData as? org.bukkit.block.data.Levelled
                        if (levelData != null && levelData.level == 0) FluidType.WATER else continue
                    }
                    Material.LAVA -> {
                        val levelData = adjacentBlock.blockData as? org.bukkit.block.data.Levelled
                        if (levelData != null && levelData.level == 0) FluidType.LAVA else continue
                    }
                    else -> continue
                }

            foundFace = face
            foundBlock = adjacentBlock
            fluidType = type
            break
        }

        if (foundFace == null || foundBlock == null) {
            pumpStatus = PumpStatus.NO_SOURCE
            return
        }

        var poweredThisTick = false
        for (neighbor in powerNeighbors) {
            if (neighbor.hasPower()) {
                val pulled = neighbor.removePower(1)
                if (pulled > 0) {
                    poweredThisTick = true
                    break
                }
            }
        }

        if (!poweredThisTick) {
            pumpStatus = PumpStatus.NO_POWER
            return
        }

        when (foundBlock.type) {
            Material.WATER_CAULDRON -> {
                val levelled = foundBlock.blockData as? Levelled
                if (levelled != null && levelled.level > 1) {
                    levelled.level = levelled.level - 1
                    foundBlock.blockData = levelled
                } else {
                    foundBlock.setType(Material.CAULDRON, false)
                }
            }
            Material.LAVA_CAULDRON -> {
                foundBlock.setType(Material.CAULDRON, false)
            }
            Material.WATER, Material.LAVA -> {
                foundBlock.setType(Material.AIR, false)
            }
            else -> {}
        }

        storeFluid(fluidType)
        cauldronFace = foundFace
        pumpStatus = PumpStatus.EXTRACTING
        plugin.logger.atlasInfo(
            "FluidPump at ${location.coordinates} extracted ${fluidType.name} from $foundFace",
        )
    }
}

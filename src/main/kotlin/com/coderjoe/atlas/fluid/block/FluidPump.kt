package com.coderjoe.atlas.fluid.block

import com.coderjoe.atlas.fluid.FluidBlock
import com.coderjoe.atlas.fluid.FluidType
import com.coderjoe.atlas.power.PowerBlockRegistry
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace

class FluidPump(location: Location) : FluidBlock(location) {

    enum class PumpStatus {
        IDLE,
        NO_SOURCE,
        NO_POWER,
        EXTRACTING
    }

    override val updateIntervalTicks: Long = 20L

    var cauldronFace: BlockFace? = null
        private set

    var isPowered: Boolean = false
        private set

    var pumpStatus: PumpStatus = PumpStatus.NO_SOURCE
        private set

    companion object {
        const val BLOCK_ID = "fluid_pump"
        const val BLOCK_ID_ACTIVE = "fluid_pump_active"
        const val BLOCK_ID_ACTIVE_LAVA = "fluid_pump_active_lava"

        private val ADJACENT_FACES = listOf(
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST,
            BlockFace.WEST, BlockFace.UP, BlockFace.DOWN
        )
    }

    fun canRemoveFluidFrom(direction: BlockFace): Boolean {
        val cauldron = cauldronFace ?: return false
        return direction == cauldron.oppositeFace && hasFluid()
    }

    override fun getVisualStateBlockId(): String = when (storedFluid) {
        FluidType.WATER -> BLOCK_ID_ACTIVE
        FluidType.LAVA -> BLOCK_ID_ACTIVE_LAVA
        FluidType.NONE -> BLOCK_ID
    }

    override fun fluidUpdate() {
        val powerRegistry = PowerBlockRegistry.instance ?: return
        val powerNeighbors = powerRegistry.getAdjacentPowerBlocks(location)
        isPowered = powerNeighbors.any { it.hasPower() }

        // Step 1: If already holding fluid, skip — no power consumed
        if (hasFluid()) {
            pumpStatus = PumpStatus.IDLE
            return
        }

        // Step 2: Scan for an adjacent cauldron
        var foundFace: BlockFace? = null
        var foundBlock: org.bukkit.block.Block? = null
        var fluidType = FluidType.NONE

        for (face in ADJACENT_FACES) {
            val offset = face.direction
            val adjacentBlock = location.world?.getBlockAt(
                location.blockX + offset.blockX,
                location.blockY + offset.blockY,
                location.blockZ + offset.blockZ
            ) ?: continue

            val type = when (adjacentBlock.type) {
                Material.WATER_CAULDRON -> FluidType.WATER
                Material.LAVA_CAULDRON -> FluidType.LAVA
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

        // Step 3: Try to pull 1 power from an adjacent power block
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

        // Step 4: Drain the cauldron and store fluid
        if (foundBlock.type == Material.WATER_CAULDRON) {
            val levelled = foundBlock.blockData as? org.bukkit.block.data.Levelled
            if (levelled != null && levelled.level > 1) {
                levelled.level = levelled.level - 1
                foundBlock.blockData = levelled
            } else {
                foundBlock.setType(Material.CAULDRON, false)
            }
        } else {
            // Lava cauldrons don't have levels
            foundBlock.setType(Material.CAULDRON, false)
        }

        storeFluid(fluidType)
        cauldronFace = foundFace
        pumpStatus = PumpStatus.EXTRACTING
        plugin.logger.info("FluidPump at ${location.blockX},${location.blockY},${location.blockZ} extracted ${fluidType.name} from $foundFace")
    }
}

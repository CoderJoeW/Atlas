package com.coderjoe.atlas.power.block

import com.coderjoe.atlas.atlasInfo
import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.PlacementType
import com.coderjoe.atlas.fluid.FluidBlockRegistry
import com.coderjoe.atlas.fluid.FluidType
import com.coderjoe.atlas.fluid.block.FluidContainer
import com.coderjoe.atlas.fluid.block.FluidMerger
import com.coderjoe.atlas.fluid.block.FluidPipe
import com.coderjoe.atlas.fluid.block.FluidPump
import com.coderjoe.atlas.power.PowerBlock
import org.bukkit.Location
import org.bukkit.block.BlockFace

class LavaGenerator(location: Location) : PowerBlock(location, maxStorage = 50) {

    override val canReceivePower: Boolean = false
    override val updateIntervalTicks: Long = 20L

    companion object {
        const val BLOCK_ID = "lava_generator"
        const val BLOCK_ID_ACTIVE = "lava_generator_active"
        const val POWER_PER_LAVA = 5

        private val ADJACENT_FACES = listOf(
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST,
            BlockFace.WEST, BlockFace.UP, BlockFace.DOWN
        )

        val descriptor = BlockDescriptor(
            baseBlockId = BLOCK_ID,
            displayName = "Lava Generator",
            description = "Generator - produces $POWER_PER_LAVA power per lava unit",
            placementType = PlacementType.SIMPLE,
            directionalVariants = emptyMap(),
            allRegistrableIds = listOf(BLOCK_ID, BLOCK_ID_ACTIVE),
            constructor = { loc, _ -> LavaGenerator(loc) }
        )
    }

    override val baseBlockId: String = BLOCK_ID

    override fun getVisualStateBlockId(): String = when {
        currentPower > 0 -> BLOCK_ID_ACTIVE
        else -> BLOCK_ID
    }

    override fun powerUpdate() {
        if (currentPower >= maxStorage) return

        val fluidRegistry = FluidBlockRegistry.instance ?: return

        for (face in ADJACENT_FACES) {
            val spaceAvailable = maxStorage - currentPower
            if (spaceAvailable < POWER_PER_LAVA) break

            val source = fluidRegistry.getAdjacentFluidBlock(location, face) ?: continue

            val lava = tryPullLava(source, face)
            if (lava) {
                val generated = addPower(POWER_PER_LAVA)
                plugin.logger.atlasInfo("LavaGenerator at ${location.blockX},${location.blockY},${location.blockZ} consumed 1 lava, generated $generated power (now $currentPower/$maxStorage)")
            }
        }
    }

    private fun tryPullLava(source: com.coderjoe.atlas.fluid.FluidBlock, face: BlockFace): Boolean {
        when (source) {
            is FluidPump -> {
                if (source.canRemoveFluidFrom(face.oppositeFace) && source.storedFluid == FluidType.LAVA) {
                    source.removeFluid()
                    return true
                }
            }
            is FluidPipe -> {
                if (source.hasFluid() && source.storedFluid == FluidType.LAVA) {
                    source.removeFluid()
                    return true
                }
            }
            is FluidContainer -> {
                if (source.canRemoveFluidFrom(face.oppositeFace) && source.storedFluid == FluidType.LAVA) {
                    source.removeFluid()
                    return true
                }
            }
            is FluidMerger -> {
                if (source.hasFluid() && source.storedFluid == FluidType.LAVA) {
                    source.removeFluid()
                    return true
                }
            }
        }
        return false
    }
}

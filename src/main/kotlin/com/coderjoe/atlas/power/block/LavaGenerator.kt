package com.coderjoe.atlas.power.block

import com.coderjoe.atlas.atlasInfo
import com.coderjoe.atlas.coordinates
import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.PlacementType
import com.coderjoe.atlas.fluid.FluidBlock
import com.coderjoe.atlas.fluid.FluidBlockRegistry
import com.coderjoe.atlas.fluid.FluidType
import com.coderjoe.atlas.power.PowerBlock
import org.bukkit.Location
import org.bukkit.block.BlockFace

class LavaGenerator(location: Location) : PowerBlock(location, maxStorage = 50) {
    override val canReceivePower: Boolean = false
    override val updateIntervalTicks: Long = 20L

    companion object {
        const val BLOCK_ID = "atlas:lava_generator"
        const val BLOCK_ID_ACTIVE = "atlas:lava_generator_active"
        const val POWER_PER_LAVA = 5

        val descriptor =
            BlockDescriptor(
                baseBlockId = BLOCK_ID,
                displayName = "Lava Generator",
                description = "Generator - produces $POWER_PER_LAVA power per lava unit",
                placementType = PlacementType.SIMPLE,
                additionalBlockIds = listOf(BLOCK_ID_ACTIVE),
                constructor = { loc, _ -> LavaGenerator(loc) },
            )
    }

    override val baseBlockId: String = BLOCK_ID

    override fun getVisualStateBlockId(): String =
        when {
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
                plugin.logger.atlasInfo(
                    "LavaGenerator at ${location.coordinates} " +
                        "consumed 1 lava, generated $generated power (now $currentPower/$maxStorage)",
                )
            }
        }
    }

    private fun tryPullLava(
        source: FluidBlock,
        face: BlockFace,
    ): Boolean {
        if (source.canProvideFluid(face.oppositeFace) && source.storedFluid == FluidType.LAVA) {
            source.removeFluid()
            return true
        }
        return false
    }
}

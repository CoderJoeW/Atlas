package com.coderjoe.atlas.fluid.block

import com.coderjoe.atlas.atlasInfo
import com.coderjoe.atlas.coordinates
import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.PlacementType
import com.coderjoe.atlas.fluid.FluidBlock
import com.coderjoe.atlas.fluid.FluidBlockRegistry
import org.bukkit.Location
import org.bukkit.block.BlockFace

class FluidMerger(location: Location, override val facing: BlockFace) : FluidBlock(location) {
    override val updateIntervalTicks: Long = 20L

    companion object {
        const val BLOCK_ID = "atlas:fluid_merger"

        val descriptor =
            BlockDescriptor(
                baseBlockId = BLOCK_ID,
                displayName = "Fluid Merger",
                description = "Merger - merges fluid from all sides, outputs in facing direction",
                placementType = PlacementType.DIRECTIONAL,
                constructor = { loc, facing -> FluidMerger(loc, facing) },
            )
    }

    override val baseBlockId: String = BLOCK_ID

    override fun getVisualStateBlockId(): String = BLOCK_ID

    override fun fluidUpdate() {
        if (hasFluid()) {
            updateFluidState()
            return
        }

        val registry = FluidBlockRegistry.instance ?: return

        val inputFaces = ADJACENT_FACES.filter { it != facing }

        for (face in inputFaces) {
            val source = registry.getAdjacentFluidBlock(location, face) ?: continue

            if (source.canProvideFluid(face.oppositeFace)) {
                val fluid = source.removeFluid()
                storeFluid(fluid)
                plugin.logger.atlasInfo(
                    "FluidMerger at ${location.coordinates} " +
                        "pulled ${fluid.name} from ${source::class.simpleName} at ${face.name}",
                )
                updateFluidState()
                return
            }
        }

        updateFluidState()
    }
}

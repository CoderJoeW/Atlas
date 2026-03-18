package com.coderjoe.atlas.fluid.block

import com.coderjoe.atlas.atlasInfo
import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.CraftEngineHelper
import com.coderjoe.atlas.core.PlacementType
import com.coderjoe.atlas.fluid.FluidBlock
import com.coderjoe.atlas.fluid.FluidBlockRegistry
import com.coderjoe.atlas.fluid.FluidType
import org.bukkit.Location
import org.bukkit.block.BlockFace

class FluidSplitter(location: Location, override val facing: BlockFace) : FluidBlock(location) {
    override val updateIntervalTicks: Long = 20L

    companion object {
        const val BLOCK_ID = "atlas:fluid_splitter"

        val descriptor =
            BlockDescriptor(
                baseBlockId = BLOCK_ID,
                displayName = "Fluid Splitter",
                description = "Splitter - distributes fluid to all adjacent faces",
                placementType = PlacementType.DIRECTIONAL,
                constructor = { loc, facing -> FluidSplitter(loc, facing) },
            )
    }

    override val baseBlockId: String = BLOCK_ID

    override fun getVisualStateBlockId(): String = BLOCK_ID

    private fun updateFluidState() {
        val fluidValue =
            when (storedFluid) {
                FluidType.WATER -> "water"
                FluidType.LAVA -> "lava"
                FluidType.NONE -> "none"
            }
        CraftEngineHelper.setStringProperty(location, "fluid", fluidValue)
    }

    override fun fluidUpdate() {
        val registry = FluidBlockRegistry.instance ?: return

        if (!hasFluid()) {
            val source = registry.getAdjacentFluidBlock(location, facing.oppositeFace)

            if (source != null && source.canProvideFluid(facing)) {
                val fluid = source.removeFluid()
                storeFluid(fluid)
                plugin.logger.atlasInfo(
                    "FluidSplitter at ${location.blockX},${location.blockY},${location.blockZ} " +
                        "pulled ${fluid.name} from ${source::class.simpleName}",
                )
            }
        }

        if (hasFluid()) {
            val outputFaces = ADJACENT_FACES.filter { it != facing.oppositeFace }

            for (face in outputFaces) {
                if (!hasFluid()) break
                val target = registry.getAdjacentFluidBlock(location, face) ?: continue
                if (!target.hasFluid()) {
                    val fluid = removeFluid()
                    if (target.storeFluid(fluid)) {
                        plugin.logger.atlasInfo(
                            "FluidSplitter at ${location.blockX},${location.blockY},${location.blockZ} " +
                                "pushed ${fluid.name} to ${target::class.simpleName} at ${face.name}",
                        )
                    } else {
                        storeFluid(fluid)
                    }
                    break
                }
            }
        }

        updateFluidState()
    }
}

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
        if (hasFluid()) {
            updateFluidState()
            return
        }

        val registry = FluidBlockRegistry.instance ?: return

        val inputFaces =
            listOf(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN)
                .filter { it != facing }

        for (face in inputFaces) {
            val source = registry.getAdjacentFluidBlock(location, face) ?: continue

            when (source) {
                is FluidPump -> {
                    if (source.canRemoveFluidFrom(face.oppositeFace) && source.hasFluid()) {
                        val fluid = source.removeFluid()
                        storeFluid(fluid)
                        plugin.logger.atlasInfo(
                            "FluidMerger at ${location.blockX},${location.blockY},${location.blockZ} " +
                                "pulled ${fluid.name} from FluidPump at ${face.name}",
                        )
                        updateFluidState()
                        return
                    }
                }
                is FluidPipe -> {
                    if (source.hasFluid()) {
                        val fluid = source.removeFluid()
                        storeFluid(fluid)
                        plugin.logger.atlasInfo(
                            "FluidMerger at ${location.blockX},${location.blockY},${location.blockZ} " +
                                "pulled ${fluid.name} from FluidPipe at ${face.name}",
                        )
                        updateFluidState()
                        return
                    }
                }
                is FluidContainer -> {
                    if (source.canRemoveFluidFrom(face.oppositeFace) && source.hasFluid()) {
                        val fluid = source.removeFluid()
                        storeFluid(fluid)
                        plugin.logger.atlasInfo(
                            "FluidMerger at ${location.blockX},${location.blockY},${location.blockZ} " +
                                "pulled ${fluid.name} from FluidContainer at ${face.name}",
                        )
                        updateFluidState()
                        return
                    }
                }
                is FluidMerger -> {
                    if (source.hasFluid()) {
                        val fluid = source.removeFluid()
                        storeFluid(fluid)
                        plugin.logger.atlasInfo(
                            "FluidMerger at ${location.blockX},${location.blockY},${location.blockZ} " +
                                "pulled ${fluid.name} from FluidMerger at ${face.name}",
                        )
                        updateFluidState()
                        return
                    }
                }
            }
        }

        updateFluidState()
    }
}

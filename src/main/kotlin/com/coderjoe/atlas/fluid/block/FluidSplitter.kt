package com.coderjoe.atlas.fluid.block

import com.coderjoe.atlas.atlasInfo
import com.coderjoe.atlas.coordinates
import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.PlacementType
import com.coderjoe.atlas.core.pullFromOpposite
import com.coderjoe.atlas.core.pushRoundRobin
import com.coderjoe.atlas.fluid.FluidBlock
import com.coderjoe.atlas.fluid.FluidBlockRegistry
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
                showFacingInDisplayName = true,
                constructor = { loc, facing -> FluidSplitter(loc, facing) },
            )
    }

    override val baseBlockId: String = BLOCK_ID
    private var nextOutputIndex: Int = 0

    override fun getVisualStateBlockId(): String = BLOCK_ID

    override fun fluidUpdate() {
        val registry = FluidBlockRegistry.instance ?: return

        pullFromOpposite(
            facing = facing,
            getAdjacent = { face -> registry.getAdjacentBlock(location, face) },
            canPullSelf = { !hasFluid() },
            canProvide = { source -> source.canProvideFluid(facing) },
            transfer = { source ->
                val fluid = source.removeFluid()
                storeFluid(fluid)
                plugin.logger.atlasInfo(
                    "FluidSplitter at ${location.coordinates} " +
                        "pulled ${fluid.name} from ${source::class.simpleName}",
                )
            },
        )

        if (hasFluid()) {
            nextOutputIndex =
                pushRoundRobin(
                    excludeFace = facing.oppositeFace,
                    startIndex = nextOutputIndex,
                    getAdjacent = { face -> registry.getAdjacentBlock(location, face) },
                    hasResource = { hasFluid() },
                    isCandidate = { target -> !target.hasFluid() },
                    tryPush = { target, face ->
                        val fluid = removeFluid()
                        if (target.storeFluid(fluid)) {
                            plugin.logger.atlasInfo(
                                "FluidSplitter at ${location.coordinates} " +
                                    "pushed ${fluid.name} to ${target::class.simpleName} at ${face.name}",
                            )
                            true
                        } else {
                            storeFluid(fluid)
                            false
                        }
                    },
                    stopAfterFirstCandidate = true,
                )
        }

        updateFluidState()
    }
}

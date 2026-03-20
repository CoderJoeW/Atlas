package com.coderjoe.atlas.fluid.block

import com.coderjoe.atlas.atlasInfo
import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.PlacementType
import com.coderjoe.atlas.fluid.FluidBlock
import com.coderjoe.atlas.fluid.FluidBlockRegistry
import org.bukkit.Location
import org.bukkit.block.BlockFace

class FluidPipe(location: Location, override val facing: BlockFace) : FluidBlock(location) {
    override val updateIntervalTicks: Long = 20L

    companion object {
        const val BLOCK_ID = "atlas:fluid_pipe"

        val descriptor =
            BlockDescriptor(
                baseBlockId = BLOCK_ID,
                displayName = "Fluid Pipe",
                description = "Pipe - transports fluid in facing direction",
                placementType = PlacementType.DIRECTIONAL,
                constructor = { loc, facing -> FluidPipe(loc, facing) },
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
        val behind = facing.oppositeFace
        val source = registry.getAdjacentFluidBlock(location, behind) ?: return

        if (source.canProvideFluid(facing)) {
            val fluid = source.removeFluid()
            storeFluid(fluid)
            plugin.logger.atlasInfo(
                "FluidPipe at ${location.blockX},${location.blockY},${location.blockZ} " +
                    "pulled ${fluid.name} from ${source::class.simpleName}",
            )
        }

        updateFluidState()
    }
}

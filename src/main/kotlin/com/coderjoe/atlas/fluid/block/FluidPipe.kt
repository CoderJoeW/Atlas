package com.coderjoe.atlas.fluid.block

import com.coderjoe.atlas.atlasInfo
import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.PlacementType
import com.coderjoe.atlas.fluid.FluidBlock
import com.coderjoe.atlas.fluid.FluidBlockRegistry
import com.coderjoe.atlas.fluid.FluidType
import org.bukkit.Location
import org.bukkit.block.BlockFace

class FluidPipe(location: Location, override val facing: BlockFace) : FluidBlock(location) {

    override val updateIntervalTicks: Long = 20L

    companion object {
        const val BLOCK_ID = "fluid_pipe"

        val DIRECTIONAL_IDS = mapOf(
            BlockFace.NORTH to "fluid_pipe_north",
            BlockFace.SOUTH to "fluid_pipe_south",
            BlockFace.EAST to "fluid_pipe_east",
            BlockFace.WEST to "fluid_pipe_west",
            BlockFace.UP to "fluid_pipe_up",
            BlockFace.DOWN to "fluid_pipe_down"
        )

        val ID_TO_FACING = DIRECTIONAL_IDS.entries.associate { (face, id) -> id to face }

        val WATER_FILLED_IDS = mapOf(
            BlockFace.NORTH to "fluid_pipe_north_filled",
            BlockFace.SOUTH to "fluid_pipe_south_filled",
            BlockFace.EAST to "fluid_pipe_east_filled",
            BlockFace.WEST to "fluid_pipe_west_filled",
            BlockFace.UP to "fluid_pipe_up_filled",
            BlockFace.DOWN to "fluid_pipe_down_filled"
        )

        val LAVA_FILLED_IDS = mapOf(
            BlockFace.NORTH to "fluid_pipe_north_filled_lava",
            BlockFace.SOUTH to "fluid_pipe_south_filled_lava",
            BlockFace.EAST to "fluid_pipe_east_filled_lava",
            BlockFace.WEST to "fluid_pipe_west_filled_lava",
            BlockFace.UP to "fluid_pipe_up_filled_lava",
            BlockFace.DOWN to "fluid_pipe_down_filled_lava"
        )

        fun facingFromBlockId(blockId: String): BlockFace? = ID_TO_FACING[blockId]

        val descriptor = BlockDescriptor(
            baseBlockId = BLOCK_ID,
            displayName = "Fluid Pipe",
            description = "Pipe - transports fluid in facing direction",
            placementType = PlacementType.DIRECTIONAL,
            directionalVariants = DIRECTIONAL_IDS,
            allRegistrableIds = DIRECTIONAL_IDS.values.toList() + WATER_FILLED_IDS.values.toList() + LAVA_FILLED_IDS.values.toList(),
            constructor = { loc, facing -> FluidPipe(loc, facing) }
        )
    }

    override val baseBlockId: String = BLOCK_ID

    override fun getVisualStateBlockId(): String = when (storedFluid) {
        FluidType.WATER -> WATER_FILLED_IDS[facing]!!
        FluidType.LAVA -> LAVA_FILLED_IDS[facing]!!
        FluidType.NONE -> DIRECTIONAL_IDS[facing]!!
    }

    override fun fluidUpdate() {
        if (hasFluid()) return

        val registry = FluidBlockRegistry.instance ?: return
        val behind = facing.oppositeFace
        val source = registry.getAdjacentFluidBlock(location, behind) ?: return

        when (source) {
            is FluidPump -> {
                if (source.canRemoveFluidFrom(facing)) {
                    val fluid = source.removeFluid()
                    storeFluid(fluid)
                    plugin.logger.atlasInfo("FluidPipe at ${location.blockX},${location.blockY},${location.blockZ} pulled ${fluid.name} from FluidPump")
                }
            }
            is FluidPipe -> {
                if (source.hasFluid()) {
                    val fluid = source.removeFluid()
                    storeFluid(fluid)
                    plugin.logger.atlasInfo("FluidPipe at ${location.blockX},${location.blockY},${location.blockZ} pulled ${fluid.name} from FluidPipe")
                }
            }
            is FluidContainer -> {
                if (source.canRemoveFluidFrom(facing)) {
                    val fluid = source.removeFluid()
                    storeFluid(fluid)
                    plugin.logger.atlasInfo("FluidPipe at ${location.blockX},${location.blockY},${location.blockZ} pulled ${fluid.name} from FluidContainer")
                }
            }
        }
    }
}

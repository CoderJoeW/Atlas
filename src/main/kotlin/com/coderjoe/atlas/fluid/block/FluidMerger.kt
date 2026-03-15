package com.coderjoe.atlas.fluid.block

import com.coderjoe.atlas.atlasInfo
import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.PlacementType
import com.coderjoe.atlas.fluid.FluidBlock
import com.coderjoe.atlas.fluid.FluidBlockRegistry
import com.coderjoe.atlas.fluid.FluidType
import org.bukkit.Location
import org.bukkit.block.BlockFace

class FluidMerger(location: Location, override val facing: BlockFace) : FluidBlock(location) {

    override val updateIntervalTicks: Long = 20L

    companion object {
        const val BLOCK_ID = "fluid_merger"

        val DIRECTIONAL_IDS = mapOf(
            BlockFace.NORTH to "fluid_merger_north",
            BlockFace.SOUTH to "fluid_merger_south",
            BlockFace.EAST to "fluid_merger_east",
            BlockFace.WEST to "fluid_merger_west",
            BlockFace.UP to "fluid_merger_up",
            BlockFace.DOWN to "fluid_merger_down"
        )

        val WATER_FILLED_IDS = mapOf(
            BlockFace.NORTH to "fluid_merger_north_filled",
            BlockFace.SOUTH to "fluid_merger_south_filled",
            BlockFace.EAST to "fluid_merger_east_filled",
            BlockFace.WEST to "fluid_merger_west_filled",
            BlockFace.UP to "fluid_merger_up_filled",
            BlockFace.DOWN to "fluid_merger_down_filled"
        )

        val LAVA_FILLED_IDS = mapOf(
            BlockFace.NORTH to "fluid_merger_north_filled_lava",
            BlockFace.SOUTH to "fluid_merger_south_filled_lava",
            BlockFace.EAST to "fluid_merger_east_filled_lava",
            BlockFace.WEST to "fluid_merger_west_filled_lava",
            BlockFace.UP to "fluid_merger_up_filled_lava",
            BlockFace.DOWN to "fluid_merger_down_filled_lava"
        )

        val ID_TO_FACING = DIRECTIONAL_IDS.entries.associate { (face, id) -> id to face }

        fun facingFromBlockId(blockId: String): BlockFace? = ID_TO_FACING[blockId]

        val descriptor = BlockDescriptor(
            baseBlockId = BLOCK_ID,
            displayName = "Fluid Merger",
            description = "Merger - merges fluid from all sides, outputs in facing direction",
            placementType = PlacementType.DIRECTIONAL,
            directionalVariants = DIRECTIONAL_IDS,
            allRegistrableIds = DIRECTIONAL_IDS.values.toList() + WATER_FILLED_IDS.values.toList() + LAVA_FILLED_IDS.values.toList(),
            constructor = { loc, facing -> FluidMerger(loc, facing) }
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

        // Pull fluid from all faces except the output (facing) direction
        val inputFaces = listOf(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN)
            .filter { it != facing }

        for (face in inputFaces) {
            val source = registry.getAdjacentFluidBlock(location, face) ?: continue

            when (source) {
                is FluidPump -> {
                    if (source.canRemoveFluidFrom(face.oppositeFace) && source.hasFluid()) {
                        val fluid = source.removeFluid()
                        storeFluid(fluid)
                        plugin.logger.atlasInfo(
                            """
                            FluidMerger at ${location.blockX},${location.blockY},${location.blockZ} pulled ${fluid.name} from FluidPump at ${face.name}
                            """.trimIndent(),
                        )
                        return
                    }
                }
                is FluidPipe -> {
                    if (source.hasFluid()) {
                        val fluid = source.removeFluid()
                        storeFluid(fluid)
                        plugin.logger.atlasInfo(
                            """
                            FluidMerger at ${location.blockX},${location.blockY},${location.blockZ} pulled ${fluid.name} from FluidPipe at ${face.name}
                            """.trimIndent(),
                        )
                        return
                    }
                }
                is FluidContainer -> {
                    if (source.canRemoveFluidFrom(face.oppositeFace) && source.hasFluid()) {
                        val fluid = source.removeFluid()
                        storeFluid(fluid)
                        plugin.logger.atlasInfo(
                            """
                            FluidMerger at ${location.blockX},${location.blockY},${location.blockZ} pulled ${fluid.name} from FluidContainer at ${face.name}
                            """.trimIndent(),
                        )
                        return
                    }
                }
                is FluidMerger -> {
                    if (source.hasFluid()) {
                        val fluid = source.removeFluid()
                        storeFluid(fluid)
                        plugin.logger.atlasInfo(
                            """
                            FluidMerger at ${location.blockX},${location.blockY},${location.blockZ} pulled ${fluid.name} from FluidMerger at ${face.name}
                            """.trimIndent(),
                        )
                        return
                    }
                }
            }
        }
    }
}

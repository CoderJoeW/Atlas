package com.coderjoe.atlas.fluid.block

import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.PlacementType
import com.coderjoe.atlas.fluid.FluidBlock
import com.coderjoe.atlas.fluid.FluidBlockRegistry
import com.coderjoe.atlas.fluid.FluidType
import org.bukkit.Location
import org.bukkit.block.BlockFace

class FluidContainer(location: Location, override val facing: BlockFace) : FluidBlock(location) {

    var storedAmount: Int = 0
        private set

    override val updateIntervalTicks: Long = 20L

    companion object {
        const val BLOCK_ID = "fluid_container"
        const val MAX_CAPACITY = 10

        val DIRECTIONAL_IDS = mapOf(
            BlockFace.NORTH to "fluid_container_north",
            BlockFace.SOUTH to "fluid_container_south",
            BlockFace.EAST to "fluid_container_east",
            BlockFace.WEST to "fluid_container_west",
            BlockFace.UP to "fluid_container_up",
            BlockFace.DOWN to "fluid_container_down"
        )

        val ID_TO_FACING = DIRECTIONAL_IDS.entries.associate { (face, id) -> id to face }

        private val FILL_LEVELS = listOf("low", "medium", "full")
        private val FLUID_TYPES = listOf("water", "lava")

        val FILLED_IDS: Map<BlockFace, Map<FluidType, Map<String, String>>> = BlockFace.values()
            .filter { DIRECTIONAL_IDS.containsKey(it) }
            .associateWith { face ->
                val dir = face.name.lowercase()
                mapOf(
                    FluidType.WATER to mapOf(
                        "low" to "fluid_container_${dir}_water_low",
                        "medium" to "fluid_container_${dir}_water_medium",
                        "full" to "fluid_container_${dir}_water_full"
                    ),
                    FluidType.LAVA to mapOf(
                        "low" to "fluid_container_${dir}_lava_low",
                        "medium" to "fluid_container_${dir}_lava_medium",
                        "full" to "fluid_container_${dir}_lava_full"
                    )
                )
            }

        val ALL_VARIANT_IDS: List<String> = buildList {
            addAll(DIRECTIONAL_IDS.values)
            for (face in FILLED_IDS.keys) {
                for (fluidMap in FILLED_IDS[face]!!.values) {
                    addAll(fluidMap.values)
                }
            }
        }

        fun facingFromBlockId(blockId: String): BlockFace? {
            ID_TO_FACING[blockId]?.let { return it }
            for ((face, fluidMap) in FILLED_IDS) {
                for (levelMap in fluidMap.values) {
                    if (blockId in levelMap.values) return face
                }
            }
            return null
        }

        val descriptor = BlockDescriptor(
            baseBlockId = BLOCK_ID,
            displayName = "Fluid Container",
            description = "Container - stores up to $MAX_CAPACITY units of fluid",
            placementType = PlacementType.DIRECTIONAL,
            directionalVariants = DIRECTIONAL_IDS,
            allRegistrableIds = ALL_VARIANT_IDS,
            constructor = { loc, facing -> FluidContainer(loc, facing) }
        )
    }

    override val baseBlockId: String = BLOCK_ID

    override fun hasFluid(): Boolean = storedAmount > 0

    override fun storeFluid(type: FluidType): Boolean {
        if (storedAmount >= MAX_CAPACITY) return false
        if (storedFluid != FluidType.NONE && storedFluid != type) return false
        storedFluid = type
        storedAmount++
        return true
    }

    override fun removeFluid(): FluidType {
        if (storedAmount <= 0) return FluidType.NONE
        val fluid = storedFluid
        storedAmount--
        if (storedAmount == 0) {
            storedFluid = FluidType.NONE
        }
        return fluid
    }

    fun canRemoveFluidFrom(direction: BlockFace): Boolean {
        return direction == facing && hasFluid()
    }

    fun getFillLevel(): String = when (storedAmount) {
        0 -> "empty"
        in 1..3 -> "low"
        in 4..7 -> "medium"
        else -> "full"
    }

    override fun getVisualStateBlockId(): String {
        if (storedAmount == 0 || storedFluid == FluidType.NONE) {
            return DIRECTIONAL_IDS[facing]!!
        }
        return FILLED_IDS[facing]!![storedFluid]!![getFillLevel()]!!
    }

    override fun fluidUpdate() {
        if (storedAmount >= MAX_CAPACITY) return

        val registry = FluidBlockRegistry.instance ?: return
        val behind = facing.oppositeFace
        val source = registry.getAdjacentFluidBlock(location, behind) ?: return

        when (source) {
            is FluidPump -> {
                if (source.canRemoveFluidFrom(facing)) {
                    val fluid = source.removeFluid()
                    if (storeFluid(fluid)) {
                        plugin.logger.info("FluidContainer at ${location.blockX},${location.blockY},${location.blockZ} pulled ${fluid.name} from FluidPump")
                    } else {
                        source.storeFluid(fluid)
                    }
                }
            }
            is FluidPipe -> {
                if (source.hasFluid()) {
                    val fluid = source.removeFluid()
                    if (storeFluid(fluid)) {
                        plugin.logger.info("FluidContainer at ${location.blockX},${location.blockY},${location.blockZ} pulled ${fluid.name} from FluidPipe")
                    } else {
                        source.storeFluid(fluid)
                    }
                }
            }
            is FluidContainer -> {
                if (source.canRemoveFluidFrom(facing)) {
                    val fluid = source.removeFluid()
                    if (storeFluid(fluid)) {
                        plugin.logger.info("FluidContainer at ${location.blockX},${location.blockY},${location.blockZ} pulled ${fluid.name} from FluidContainer")
                    } else {
                        source.storeFluid(fluid)
                    }
                }
            }
        }
    }

    fun restoreState(type: FluidType, amount: Int) {
        storedFluid = type
        storedAmount = amount.coerceIn(0, MAX_CAPACITY)
    }
}

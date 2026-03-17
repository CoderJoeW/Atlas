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

class FluidContainer(location: Location, override val facing: BlockFace) : FluidBlock(location) {
    var storedAmount: Int = 0
        private set

    override val updateIntervalTicks: Long = 20L

    companion object {
        const val BLOCK_ID = "atlas:fluid_container"
        const val MAX_CAPACITY = 10

        val descriptor =
            BlockDescriptor(
                baseBlockId = BLOCK_ID,
                displayName = "Fluid Container",
                description = "Container - stores up to $MAX_CAPACITY units of fluid",
                placementType = PlacementType.DIRECTIONAL,
                constructor = { loc, facing -> FluidContainer(loc, facing) },
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

    override fun canProvideFluid(requestDirection: BlockFace): Boolean = canRemoveFluidFrom(requestDirection)

    fun getFillLevel(): Int =
        when (storedAmount) {
            0 -> 0
            in 1..3 -> 1
            in 4..7 -> 2
            else -> 3
        }

    override fun getVisualStateBlockId(): String = BLOCK_ID

    private fun updateProperties() {
        val fluidValue =
            when (storedFluid) {
                FluidType.WATER -> "water"
                FluidType.LAVA -> "lava"
                FluidType.NONE -> "none"
            }
        CraftEngineHelper.setStringProperty(location, "fluid", fluidValue)
        CraftEngineHelper.setIntProperty(location, "fill_level", getFillLevel())
    }

    override fun fluidUpdate() {
        if (storedAmount >= MAX_CAPACITY) {
            updateProperties()
            return
        }

        val registry = FluidBlockRegistry.instance ?: return
        val behind = facing.oppositeFace
        val source =
            registry.getAdjacentFluidBlock(location, behind) ?: run {
                updateProperties()
                return
            }

        if (source.canProvideFluid(facing)) {
            val fluid = source.removeFluid()
            if (storeFluid(fluid)) {
                plugin.logger.atlasInfo(
                    "FluidContainer at ${location.blockX},${location.blockY},${location.blockZ} " +
                        "pulled ${fluid.name} from ${source::class.simpleName}",
                )
            } else {
                source.storeFluid(fluid)
            }
        }

        updateProperties()
    }

    fun restoreState(
        type: FluidType,
        amount: Int,
    ) {
        storedFluid = type
        storedAmount = amount.coerceIn(0, MAX_CAPACITY)
    }
}

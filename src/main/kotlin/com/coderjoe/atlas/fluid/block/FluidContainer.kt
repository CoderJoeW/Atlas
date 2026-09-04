package com.coderjoe.atlas.fluid.block

import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.CraftEngineHelper
import com.coderjoe.atlas.core.PlacementType
import com.coderjoe.atlas.fluid.FluidBlock
import com.coderjoe.atlas.fluid.FluidType
import org.bukkit.Location
import org.bukkit.block.BlockFace

/**
 * A tank. It holds fluid and does nothing else.
 *
 * It used to have a facing that fixed both ends of it - an inlet at the back, an outlet at the
 * front - and it reached into whatever sat behind it and drained it. Nothing else in the fluid
 * system works that way any more: a pump hands its own unit on, and a pipe run delivers to
 * whatever will take it. A tank that also pulled meant a unit could move twice in a tick, and a
 * tank placed the wrong way round quietly refused to fill.
 *
 * So it is passive and has no facing, the same shape the Small Battery settled into. It takes
 * fluid in through any side, gives it out through any side, and never moves anything itself.
 */
class FluidContainer(location: Location) : FluidBlock(location) {
    var storedAmount: Int = 0
        private set

    override val updateIntervalTicks: Long = 20L

    companion object {
        const val BLOCK_ID = "atlas:fluid_container"
        const val MAX_CAPACITY = 20

        /** Bars on the level gauge. Divides [MAX_CAPACITY] exactly, so each bar is worth four. */
        const val FILL_LEVELS = 5

        val descriptor =
            BlockDescriptor(
                baseBlockId = BLOCK_ID,
                displayName = "Fluid Container",
                description = "Tank - holds up to $MAX_CAPACITY units of fluid, filled from any side",
                placementType = PlacementType.SIMPLE,
                constructor = { loc, _ -> FluidContainer(loc) },
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

    /** Gives out through any side, so a machine draws from a tank whichever way it is built. */
    override fun canProvideFluid(requestDirection: BlockFace): Boolean = hasFluid()

    /** Takes in through any side, while there is room and the fluid matches what is already in. */
    override fun canAcceptFluid(
        face: BlockFace,
        type: FluidType,
    ): Boolean {
        if (storedAmount >= MAX_CAPACITY) return false
        return type == FluidType.NONE || storedFluid == FluidType.NONE || storedFluid == type
    }

    /** Which bar the gauge reads up to. Anything above empty shows at least one. */
    fun getFillLevel(): Int {
        if (storedAmount <= 0) return 0
        val perLevel = MAX_CAPACITY / FILL_LEVELS
        return ((storedAmount + perLevel - 1) / perLevel).coerceAtMost(FILL_LEVELS)
    }

    override fun getVisualStateBlockId(): String = BLOCK_ID

    /** A tank moves nothing of its own; all it does on a tick is show what it is holding. */
    override fun fluidUpdate() {
        updateFluidState()
        CraftEngineHelper.setIntProperty(location, "fill_level", getFillLevel())
    }

    fun restoreState(
        type: FluidType,
        amount: Int,
    ) {
        storedFluid = type
        storedAmount = amount.coerceIn(0, MAX_CAPACITY)
    }
}

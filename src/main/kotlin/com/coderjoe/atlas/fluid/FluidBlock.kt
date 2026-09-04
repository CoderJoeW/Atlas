package com.coderjoe.atlas.fluid

import com.coderjoe.atlas.core.AtlasBlock
import com.coderjoe.atlas.core.BlockRegistry
import com.coderjoe.atlas.core.CraftEngineHelper
import org.bukkit.Location
import org.bukkit.block.BlockFace

abstract class FluidBlock(
    location: Location,
    var storedFluid: FluidType = FluidType.NONE,
) : AtlasBlock(location) {
    open fun hasFluid(): Boolean = storedFluid != FluidType.NONE

    protected fun updateFluidState() {
        CraftEngineHelper.setStringProperty(location, "fluid", storedFluid.name.lowercase())
    }

    open fun canProvideFluid(requestDirection: BlockFace): Boolean = hasFluid()

    /**
     * Whether this block moves its own fluid out rather than waiting for a run to pull from it.
     *
     * A run skips these when it looks for something to drain, so the unit is not moved twice. It
     * still counts as a source everywhere else - what a run is carrying, and which pipes belong
     * to which fluid, are both read off the blocks feeding it.
     */
    open val pushesFluid: Boolean = false

    /**
     * Whether this block would take a unit of [type] pushed in through [face], where [face]
     * points from this block toward the pusher.
     *
     * [FluidType.NONE] means "any fluid at all" and is what a pipe run asks when it is only
     * looking for somewhere to send things. Blocks with a designated inlet override this; by
     * default a block takes fluid from any side as long as it has room.
     */
    open fun canAcceptFluid(
        face: BlockFace,
        type: FluidType = FluidType.NONE,
    ): Boolean = !hasFluid()

    open fun storeFluid(type: FluidType): Boolean {
        if (storedFluid != FluidType.NONE) return false
        storedFluid = type
        return true
    }

    open fun removeFluid(): FluidType {
        val fluid = storedFluid
        storedFluid = FluidType.NONE
        return fluid
    }

    protected abstract fun fluidUpdate()

    override fun blockUpdate() {
        fluidUpdate()
    }

    override fun getRegistry(): BlockRegistry<*> {
        return FluidBlockRegistry.instance ?: throw IllegalStateException("FluidBlockRegistry not initialized")
    }
}

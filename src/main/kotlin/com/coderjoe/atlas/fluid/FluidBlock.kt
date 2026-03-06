package com.coderjoe.atlas.fluid

import com.coderjoe.atlas.core.AtlasBlock
import com.coderjoe.atlas.core.BlockRegistry
import org.bukkit.Location

abstract class FluidBlock(
    location: Location,
    var storedFluid: FluidType = FluidType.NONE
) : AtlasBlock(location) {

    open fun hasFluid(): Boolean = storedFluid != FluidType.NONE

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

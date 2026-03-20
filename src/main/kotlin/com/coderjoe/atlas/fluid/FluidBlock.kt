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

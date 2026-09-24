package com.coderjoe.atlas.testing

import com.coderjoe.atlas.block.AtlasBlock
import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.block.fluid.FluidBlockFactory
import com.coderjoe.atlas.block.fluid.FluidContainer
import com.coderjoe.atlas.block.fluid.FluidPipe
import com.coderjoe.atlas.block.fluid.FluidPump
import com.coderjoe.atlas.block.power.LavaGenerator
import com.coderjoe.atlas.block.power.PowerBlockFactory
import com.coderjoe.atlas.block.power.PowerCable
import com.coderjoe.atlas.block.power.SmallBattery
import com.coderjoe.atlas.block.power.SmallSolarPanel
import com.coderjoe.atlas.block.power.factory.CobblestoneFactory
import com.coderjoe.atlas.block.power.factory.ObsidianFactory
import com.coderjoe.atlas.block.power.mine.MineTier
import com.coderjoe.atlas.block.transport.ConveyorBelt
import com.coderjoe.atlas.block.transport.TransportBlockFactory

/**
 * Fixtures for laying out blocks by hand and filling the per-system factories. A test drives a
 * block's update by calling its internal hook - `powerUpdate()`, `fluidUpdate()` - directly.
 *
 * Blocks themselves are placed with [com.coderjoe.atlas.block.BlockRegistry.track], which gives
 * them their context without starting their tick tasks.
 */
object Blocks {
    /** Tracks this block in [registry] under its own id and hands it back, for tests that need one in place. */
    fun <T : AtlasBlock> T.placedIn(registry: BlockRegistry): T {
        registry.track(this, baseBlockId)
        return this
    }

    fun initPowerFactory() {
        PowerBlockFactory.registerFromDescriptors(
            listOf(
                SmallSolarPanel.descriptor,
                SmallBattery.descriptor,
                PowerCable.descriptor,
                LavaGenerator.descriptor,
                CobblestoneFactory.descriptor,
                ObsidianFactory.descriptor,
                *MineTier.entries.map { it.descriptor }.toTypedArray(),
            ),
        )
    }

    fun initFluidFactory() {
        FluidBlockFactory.registerFromDescriptors(
            listOf(
                FluidPump.descriptor,
                FluidPipe.descriptor,
                FluidContainer.descriptor,
            ),
        )
    }

    fun initTransportFactory() {
        TransportBlockFactory.registerFromDescriptors(
            listOf(
                ConveyorBelt.descriptor,
            ),
        )
    }

    fun clearFactories() {
        PowerBlockFactory.clear()
        FluidBlockFactory.clear()
        TransportBlockFactory.clear()
    }
}

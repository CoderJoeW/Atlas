package com.coderjoe.atlas

import com.coderjoe.atlas.block.BlockCatalog
import com.coderjoe.atlas.block.fluid.FluidContainer
import com.coderjoe.atlas.block.fluid.FluidPipe
import com.coderjoe.atlas.block.fluid.FluidPump
import com.coderjoe.atlas.block.power.LavaGenerator
import com.coderjoe.atlas.block.power.PowerCable
import com.coderjoe.atlas.block.power.SmallBattery
import com.coderjoe.atlas.block.power.SmallSolarPanel
import com.coderjoe.atlas.block.power.factory.CobblestoneFactory
import com.coderjoe.atlas.block.power.factory.ObsidianFactory
import com.coderjoe.atlas.block.power.mine.MineTier
import com.coderjoe.atlas.block.transport.ConveyorBelt

object AtlasBlockTypes {
    val catalog =
        BlockCatalog(
            listOf(
                SmallSolarPanel.descriptor,
                SmallBattery.descriptor,
                PowerCable.descriptor,
                LavaGenerator.descriptor,
                CobblestoneFactory.descriptor,
                ObsidianFactory.descriptor,
                *MineTier.entries.map { it.descriptor }.toTypedArray(),
                FluidPump.descriptor,
                FluidPipe.descriptor,
                FluidContainer.descriptor,
                ConveyorBelt.descriptor,
            ),
        )
}

package com.coderjoe.atlas

import com.coderjoe.atlas.block.BlockCatalog
import com.coderjoe.atlas.block.fluid.block.FluidContainer
import com.coderjoe.atlas.block.fluid.block.FluidPipe
import com.coderjoe.atlas.block.fluid.block.FluidPump
import com.coderjoe.atlas.block.power.LavaGenerator
import com.coderjoe.atlas.block.power.PowerCable
import com.coderjoe.atlas.block.power.SmallBattery
import com.coderjoe.atlas.block.power.SmallSolarPanel
import com.coderjoe.atlas.block.power.factory.CobblestoneFactory
import com.coderjoe.atlas.block.power.factory.ObsidianFactory
import com.coderjoe.atlas.block.power.mine.CoalMine
import com.coderjoe.atlas.block.power.mine.DiamondMine
import com.coderjoe.atlas.block.power.mine.EmeraldMine
import com.coderjoe.atlas.block.power.mine.GoldMine
import com.coderjoe.atlas.block.power.mine.IronMine
import com.coderjoe.atlas.block.power.mine.NetheriteMine
import com.coderjoe.atlas.block.power.mine.RedstoneMine
import com.coderjoe.atlas.block.transport.block.ConveyorBelt

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
                CoalMine.descriptor,
                IronMine.descriptor,
                RedstoneMine.descriptor,
                GoldMine.descriptor,
                EmeraldMine.descriptor,
                DiamondMine.descriptor,
                NetheriteMine.descriptor,
                FluidPump.descriptor,
                FluidPipe.descriptor,
                FluidContainer.descriptor,
                ConveyorBelt.descriptor,
            ),
        )
}

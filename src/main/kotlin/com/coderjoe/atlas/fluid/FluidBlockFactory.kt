package com.coderjoe.atlas.fluid

import com.coderjoe.atlas.core.BlockFactory
import org.bukkit.Location
import org.bukkit.block.BlockFace

object FluidBlockFactory : BlockFactory<FluidBlock>() {

    fun createFluidBlock(blockId: String, location: Location, facing: BlockFace = BlockFace.SELF): FluidBlock? {
        return create(blockId, location, facing)
    }
}

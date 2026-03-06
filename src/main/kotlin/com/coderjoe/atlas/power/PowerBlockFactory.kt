package com.coderjoe.atlas.power

import com.coderjoe.atlas.core.BlockFactory
import org.bukkit.Location
import org.bukkit.block.BlockFace

object PowerBlockFactory : BlockFactory<PowerBlock>() {

    fun createPowerBlock(blockId: String, location: Location, facing: BlockFace = BlockFace.SELF): PowerBlock? {
        return create(blockId, location, facing)
    }
}

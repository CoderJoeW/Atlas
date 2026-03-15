package com.coderjoe.atlas.transport

import com.coderjoe.atlas.core.BlockFactory
import org.bukkit.Location
import org.bukkit.block.BlockFace

object TransportBlockFactory : BlockFactory<TransportBlock>() {
    fun createTransportBlock(
        blockId: String,
        location: Location,
        facing: BlockFace = BlockFace.SELF,
    ): TransportBlock? {
        return create(blockId, location, facing)
    }
}

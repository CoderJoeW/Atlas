package com.coderjoe.atlas.block.transport

import com.coderjoe.atlas.block.AtlasBlock
import org.bukkit.Location

abstract class TransportBlock(
    location: Location,
) : AtlasBlock(location) {
    internal abstract fun transportUpdate()

    override fun blockUpdate() {
        transportUpdate()
    }
}

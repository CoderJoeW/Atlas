package com.coderjoe.atlas.transport

import com.coderjoe.atlas.core.AtlasBlock
import com.coderjoe.atlas.core.BlockRegistry
import org.bukkit.Location

abstract class TransportBlock(
    location: Location
) : AtlasBlock(location) {

    protected abstract fun transportUpdate()

    override fun blockUpdate() {
        transportUpdate()
    }

    override fun getRegistry(): BlockRegistry<*> {
        return TransportBlockRegistry.instance ?: throw IllegalStateException("TransportBlockRegistry not initialized")
    }
}

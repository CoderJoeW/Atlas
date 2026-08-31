package com.coderjoe.atlas.transport

import com.coderjoe.atlas.core.BlockRegistry
import com.coderjoe.atlas.core.InstanceHolder
import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.bukkit.plugin.java.JavaPlugin

class TransportBlockRegistry(plugin: JavaPlugin) : BlockRegistry<TransportBlock>(plugin) {
    companion object : InstanceHolder<TransportBlockRegistry>()

    init {
        instance = this
    }

    fun registerTransportBlock(
        block: TransportBlock,
        blockId: String,
    ) = register(block, blockId)

    fun unregisterTransportBlock(location: Location): TransportBlock? = unregister(location)

    fun getTransportBlock(location: Location): TransportBlock? = getBlock(location)

    fun getAdjacentTransportBlock(
        location: Location,
        face: BlockFace,
    ): TransportBlock? = getAdjacentBlock(location, face)

    fun getAllTransportBlocksWithIds(): List<Pair<TransportBlock, String>> = getAllBlocksWithIds()
}

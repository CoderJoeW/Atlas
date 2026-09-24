package com.coderjoe.atlas.data

import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.block.transport.TransportBlock
import com.coderjoe.atlas.block.transport.TransportBlockFactory
import org.bukkit.plugin.java.JavaPlugin

class TransportBlockPersistence(plugin: JavaPlugin) : BlockPersister {
    private val persistence =
        BlockPersistence(
            plugin = plugin,
            fileName = "transport_blocks.yml",
            yamlKey = "transport_blocks",
            factory = TransportBlockFactory,
            owns = { it is TransportBlock },
        )

    override fun save(registry: BlockRegistry) = persistence.save(registry)

    override fun load(registry: BlockRegistry) = persistence.load(registry)
}

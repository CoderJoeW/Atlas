package com.coderjoe.atlas.data

import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.block.transport.TransportBlock
import com.coderjoe.atlas.block.transport.TransportBlockFactory
import org.bukkit.plugin.java.JavaPlugin

class TransportBlockPersistence(plugin: JavaPlugin) : BlockPersister<TransportBlock> {
    private val persistence =
        BlockPersistence<TransportBlock>(
            plugin = plugin,
            fileName = "transport_blocks.yml",
            yamlKey = "transport_blocks",
            factory = TransportBlockFactory,
            serialize = { _, _ -> emptyMap() },
            restore = { _, _ -> },
        )

    override fun save(registry: BlockRegistry<TransportBlock>) = persistence.save(registry)

    override fun load(registry: BlockRegistry<TransportBlock>) = persistence.load(registry)
}
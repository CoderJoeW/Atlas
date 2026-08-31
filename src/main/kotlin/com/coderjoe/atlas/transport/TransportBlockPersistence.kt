package com.coderjoe.atlas.transport

import com.coderjoe.atlas.core.BlockPersistence
import com.coderjoe.atlas.core.BlockPersister
import com.coderjoe.atlas.core.BlockRegistry
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

package com.coderjoe.atlas.transport

import com.coderjoe.atlas.core.BlockPersistence
import org.bukkit.plugin.java.JavaPlugin

class TransportBlockPersistence(plugin: JavaPlugin) {
    private val persistence = BlockPersistence<TransportBlock>(
        plugin = plugin,
        fileName = "transport_blocks.yml",
        yamlKey = "transport_blocks",
        factory = TransportBlockFactory,
        serialize = { _, _ -> emptyMap() },
        restore = { _, _ -> }
    )

    fun save(registry: TransportBlockRegistry) = persistence.save(registry)
    fun load(registry: TransportBlockRegistry) = persistence.load(registry)
}

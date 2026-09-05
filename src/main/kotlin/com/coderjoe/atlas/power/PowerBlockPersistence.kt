package com.coderjoe.atlas.power

import com.coderjoe.atlas.core.BlockPersistence
import com.coderjoe.atlas.core.BlockPersister
import com.coderjoe.atlas.core.BlockRegistry
import org.bukkit.plugin.java.JavaPlugin

class PowerBlockPersistence(plugin: JavaPlugin) : BlockPersister<PowerBlock> {
    private val persistence =
        BlockPersistence<PowerBlock>(
            plugin = plugin,
            fileName = "power_blocks.yml",
            yamlKey = "power_blocks",
            factory = PowerBlockFactory,
            serialize = { block, _ ->
                mutableMapOf<String, Any>(
                    "currentPower" to block.currentPower,
                )
            },
            restore = { block, data ->
                block.currentPower = (data["currentPower"] as? Number)?.toInt() ?: 0
            },
        )

    override fun save(registry: BlockRegistry<PowerBlock>) = persistence.save(registry)

    override fun load(registry: BlockRegistry<PowerBlock>) = persistence.load(registry)
}

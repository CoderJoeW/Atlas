package com.coderjoe.atlas.data

import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.block.power.PowerBlock
import com.coderjoe.atlas.block.power.PowerBlockFactory
import org.bukkit.plugin.java.JavaPlugin

class PowerBlockPersistence(plugin: JavaPlugin) : BlockPersister {
    private val persistence =
        BlockPersistence(
            plugin = plugin,
            fileName = "power_blocks.yml",
            yamlKey = "power_blocks",
            factory = PowerBlockFactory,
            owns = { it is PowerBlock },
        )

    override fun save(registry: BlockRegistry) = persistence.save(registry)

    override fun load(registry: BlockRegistry) = persistence.load(registry)
}

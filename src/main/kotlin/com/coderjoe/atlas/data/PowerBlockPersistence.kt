package com.coderjoe.atlas.data

import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.block.power.PowerBlock
import com.coderjoe.atlas.block.power.PowerBlockFactory
import com.coderjoe.atlas.block.power.factory.MaterialFactory
import org.bukkit.plugin.java.JavaPlugin

class PowerBlockPersistence(plugin: JavaPlugin) : BlockPersister<PowerBlock> {
    private val persistence =
        BlockPersistence<PowerBlock>(
            plugin = plugin,
            fileName = "power_blocks.yml",
            yamlKey = "power_blocks",
            factory = PowerBlockFactory,
        )

    override fun save(registry: BlockRegistry<PowerBlock>) = persistence.save(registry)

    override fun load(registry: BlockRegistry<PowerBlock>) = persistence.load(registry)
}

package com.coderjoe.atlas.data

import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.block.fluid.FluidBlock
import com.coderjoe.atlas.block.fluid.FluidBlockFactory
import org.bukkit.plugin.java.JavaPlugin

class FluidBlockPersistence(plugin: JavaPlugin) : BlockPersister {
    private val persistence =
        BlockPersistence(
            plugin = plugin,
            fileName = "fluid_blocks.yml",
            yamlKey = "fluid_blocks",
            factory = FluidBlockFactory,
            owns = { it is FluidBlock },
        )

    override fun save(registry: BlockRegistry) = persistence.save(registry)

    override fun load(registry: BlockRegistry) = persistence.load(registry)
}

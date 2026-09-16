package com.coderjoe.atlas.data

import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.block.capability.FluidType
import com.coderjoe.atlas.block.fluid.FluidBlock
import com.coderjoe.atlas.block.fluid.FluidBlockFactory
import com.coderjoe.atlas.block.fluid.block.FluidContainer
import com.coderjoe.atlas.block.fluid.block.FluidPump
import org.bukkit.plugin.java.JavaPlugin

class FluidBlockPersistence(plugin: JavaPlugin) : BlockPersister<FluidBlock> {
    private val persistence =
        BlockPersistence<FluidBlock>(
            plugin = plugin,
            fileName = "fluid_blocks.yml",
            yamlKey = "fluid_blocks",
            factory = FluidBlockFactory,
        )

    override fun save(registry: BlockRegistry<FluidBlock>) = persistence.save(registry)

    override fun load(registry: BlockRegistry<FluidBlock>) = persistence.load(registry)
}

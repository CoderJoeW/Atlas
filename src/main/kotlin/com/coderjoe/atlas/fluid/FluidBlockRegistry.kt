package com.coderjoe.atlas.fluid

import com.coderjoe.atlas.core.BlockRegistry
import com.coderjoe.atlas.core.InstanceHolder
import org.bukkit.plugin.java.JavaPlugin

class FluidBlockRegistry(plugin: JavaPlugin) : BlockRegistry<FluidBlock>(plugin) {
    companion object : InstanceHolder<FluidBlockRegistry>()

    init {
        instance = this
    }
}

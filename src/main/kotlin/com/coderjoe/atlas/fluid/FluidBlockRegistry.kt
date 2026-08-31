package com.coderjoe.atlas.fluid

import com.coderjoe.atlas.core.BlockRegistry
import org.bukkit.plugin.java.JavaPlugin

class FluidBlockRegistry(plugin: JavaPlugin) : BlockRegistry<FluidBlock>(plugin) {
    companion object {
        var instance: FluidBlockRegistry? = null
            private set
    }

    init {
        instance = this
    }
}

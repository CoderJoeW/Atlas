package com.coderjoe.atlas.power

import com.coderjoe.atlas.core.BlockRegistry
import org.bukkit.plugin.java.JavaPlugin

class PowerBlockRegistry(plugin: JavaPlugin) : BlockRegistry<PowerBlock>(plugin) {
    companion object {
        var instance: PowerBlockRegistry? = null
            private set
    }

    init {
        instance = this
    }
}

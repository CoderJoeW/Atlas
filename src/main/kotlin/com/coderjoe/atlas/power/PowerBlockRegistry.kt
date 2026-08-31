package com.coderjoe.atlas.power

import com.coderjoe.atlas.core.BlockRegistry
import com.coderjoe.atlas.core.InstanceHolder
import org.bukkit.plugin.java.JavaPlugin

class PowerBlockRegistry(plugin: JavaPlugin) : BlockRegistry<PowerBlock>(plugin) {
    companion object : InstanceHolder<PowerBlockRegistry>()

    init {
        instance = this
    }
}

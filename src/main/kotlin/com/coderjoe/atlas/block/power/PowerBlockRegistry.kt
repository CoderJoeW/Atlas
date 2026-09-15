package com.coderjoe.atlas.block.power

import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.block.InstanceHolder
import org.bukkit.plugin.java.JavaPlugin

class PowerBlockRegistry(plugin: JavaPlugin) : BlockRegistry<PowerBlock>(plugin) {
    companion object : InstanceHolder<PowerBlockRegistry>()

    init {
        instance = this
    }
}

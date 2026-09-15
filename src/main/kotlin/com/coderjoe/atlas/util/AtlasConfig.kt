package com.coderjoe.atlas.util

import org.bukkit.plugin.java.JavaPlugin

object AtlasConfig {
    var loggingEnabled: Boolean = true
        private set

    fun load(plugin: JavaPlugin) {
        plugin.saveDefaultConfig()
        loggingEnabled = plugin.config.getBoolean("logging", true)
    }
}

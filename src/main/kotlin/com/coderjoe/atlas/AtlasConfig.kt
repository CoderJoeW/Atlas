package com.coderjoe.atlas

import org.bukkit.Location
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Logger

object AtlasConfig {
    var loggingEnabled: Boolean = true
        private set

    fun load(plugin: JavaPlugin) {
        plugin.saveDefaultConfig()
        loggingEnabled = plugin.config.getBoolean("logging", true)
    }
}

fun Logger.atlasInfo(message: String) {
    if (AtlasConfig.loggingEnabled) {
        info(message)
    }
}

val Location.coordinates: String
    get() = "$blockX,$blockY,$blockZ"

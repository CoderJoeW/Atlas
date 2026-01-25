package com.coderjoe.atlas

import org.bukkit.plugin.java.JavaPlugin

class Atlas : JavaPlugin() {
    private lateinit var resourcePackManager: ResourcePackManager
    private lateinit var nexoIntegration: NexoIntegration

    override fun onEnable() {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }

        nexoIntegration = NexoIntegration(this)
        nexoIntegration.initialize()

        resourcePackManager = ResourcePackManager(this)
        resourcePackManager.load()

        if (resourcePackManager.isConfigured()) {
            logger.info("Resource pack is configured and will be sent to players on join")
            server.pluginManager.registerEvents(PlayerJoinListener(resourcePackManager), this)
        }

        logger.info("Atlas plugin enabled!")
    }

    override fun onDisable() {
        logger.info("Atlas plugin has been disabled!")
    }
}

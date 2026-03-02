package com.coderjoe.atlas

import com.coderjoe.atlas.integration.NexoIntegration
import com.coderjoe.atlas.integration.ResourcePackManager
import com.coderjoe.atlas.listener.PlayerJoinListener
import com.coderjoe.atlas.listener.PowerBlockListener
import com.coderjoe.atlas.power.PowerBlockInitializer
import com.coderjoe.atlas.power.PowerBlockRegistry
import com.coderjoe.atlas.power.persistence.PowerBlockPersistence
import com.coderjoe.atlas.ui.PowerBlockDialog
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask

class Atlas : JavaPlugin() {
    private lateinit var resourcePackManager: ResourcePackManager
    private lateinit var nexoIntegration: NexoIntegration
    private lateinit var powerBlockRegistry: PowerBlockRegistry
    private lateinit var powerBlockPersistence: PowerBlockPersistence
    private var autoSaveTask: BukkitTask? = null

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

        PowerBlockDialog.init(this)

        initPowerSystem()

        logger.info("Atlas plugin enabled!")
    }

    override fun onDisable() {
        // Cancel auto-save task
        autoSaveTask?.cancel()

        // Save all power blocks before shutdown
        if (::powerBlockPersistence.isInitialized && ::powerBlockRegistry.isInitialized) {
            powerBlockPersistence.save(powerBlockRegistry)
        }

        // Cancel all active dialog refresh tasks
        PowerBlockDialog.cleanup()

        // Stop all power block ticking
        if (::powerBlockRegistry.isInitialized) {
            powerBlockRegistry.stopAll()
        }

        logger.info("Atlas plugin has been disabled!")
    }

    fun initPowerSystem() {
        PowerBlockInitializer.initialize(this)
        powerBlockRegistry = PowerBlockRegistry(this)
        powerBlockPersistence = PowerBlockPersistence(this)
        powerBlockPersistence.load(powerBlockRegistry)

        server.pluginManager.registerEvents(PowerBlockListener(this, powerBlockRegistry), this)

        // Auto-save every 5 minutes (6000 ticks)
        autoSaveTask = server.scheduler.runTaskTimer(this, Runnable {
            powerBlockPersistence.save(powerBlockRegistry)
        }, 6000L, 6000L)

        logger.info("Power system initialized")
    }
}

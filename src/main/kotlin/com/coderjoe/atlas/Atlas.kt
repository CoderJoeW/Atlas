package com.coderjoe.atlas

import com.coderjoe.atlas.fluid.FluidBlockDialog
import com.coderjoe.atlas.fluid.FluidBlockInitializer
import com.coderjoe.atlas.fluid.FluidBlockListener
import com.coderjoe.atlas.fluid.FluidBlockPersistence
import com.coderjoe.atlas.fluid.FluidBlockRegistry
import com.coderjoe.atlas.power.PowerBlockDialog
import com.coderjoe.atlas.power.PowerBlockInitializer
import com.coderjoe.atlas.power.PowerBlockListener
import com.coderjoe.atlas.power.PowerBlockPersistence
import com.coderjoe.atlas.power.PowerBlockRegistry
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask

class Atlas : JavaPlugin() {
    private lateinit var resourcePackManager: ResourcePackManager
    private lateinit var nexoIntegration: NexoIntegration
    private lateinit var powerBlockRegistry: PowerBlockRegistry
    private lateinit var powerBlockPersistence: PowerBlockPersistence
    private lateinit var fluidBlockRegistry: FluidBlockRegistry
    private lateinit var fluidBlockPersistence: FluidBlockPersistence
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
        FluidBlockDialog.init(this)

        initPowerSystem()
        initFluidSystem()

        // Auto-save every 5 minutes (6000 ticks)
        autoSaveTask = server.scheduler.runTaskTimer(this, Runnable {
            powerBlockPersistence.save(powerBlockRegistry)
            fluidBlockPersistence.save(fluidBlockRegistry)
        }, 6000L, 6000L)

        logger.info("Atlas plugin enabled!")
    }

    override fun onDisable() {
        autoSaveTask?.cancel()

        if (::powerBlockPersistence.isInitialized && ::powerBlockRegistry.isInitialized) {
            powerBlockPersistence.save(powerBlockRegistry)
        }

        if (::fluidBlockPersistence.isInitialized && ::fluidBlockRegistry.isInitialized) {
            fluidBlockPersistence.save(fluidBlockRegistry)
        }

        PowerBlockDialog.cleanup()
        FluidBlockDialog.cleanup()

        if (::powerBlockRegistry.isInitialized) {
            powerBlockRegistry.stopAll()
        }

        if (::fluidBlockRegistry.isInitialized) {
            fluidBlockRegistry.stopAll()
        }

        logger.info("Atlas plugin has been disabled!")
    }

    fun initPowerSystem() {
        PowerBlockInitializer.initialize(this)
        powerBlockRegistry = PowerBlockRegistry(this)
        powerBlockPersistence = PowerBlockPersistence(this)
        powerBlockPersistence.load(powerBlockRegistry)

        server.pluginManager.registerEvents(PowerBlockListener(this, powerBlockRegistry), this)

        logger.info("Power system initialized")
    }

    fun initFluidSystem() {
        FluidBlockInitializer.initialize(this)
        fluidBlockRegistry = FluidBlockRegistry(this)
        fluidBlockPersistence = FluidBlockPersistence(this)
        fluidBlockPersistence.load(fluidBlockRegistry)

        server.pluginManager.registerEvents(FluidBlockListener(this, fluidBlockRegistry), this)

        logger.info("Fluid system initialized")
    }
}

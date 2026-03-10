package com.coderjoe.atlas

import com.coderjoe.atlas.core.AtlasBlockListener
import com.coderjoe.atlas.core.BlockSystem
import com.coderjoe.atlas.fluid.FluidBlockDialog
import com.coderjoe.atlas.fluid.FluidBlockFactory
import com.coderjoe.atlas.fluid.FluidBlockPersistence
import com.coderjoe.atlas.fluid.FluidBlockRegistry
import com.coderjoe.atlas.power.PowerBlockDialog
import com.coderjoe.atlas.power.PowerBlockFactory
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

        AtlasConfig.load(this)

        nexoIntegration = NexoIntegration(this)
        nexoIntegration.initialize()

        resourcePackManager = ResourcePackManager(this)
        resourcePackManager.load()

        if (resourcePackManager.isConfigured()) {
            logger.atlasInfo("Resource pack is configured and will be sent to players on join")
            server.pluginManager.registerEvents(PlayerJoinListener(resourcePackManager), this)
        }

        PowerBlockDialog.init(this)
        FluidBlockDialog.init(this)

        initPowerSystem()
        initFluidSystem()

        // Register unified listener
        val powerSystem = BlockSystem<com.coderjoe.atlas.power.PowerBlock>(
            name = "power",
            registry = powerBlockRegistry,
            factory = PowerBlockFactory,
            descriptors = powerDescriptors(),
            showDialog = { player, block ->
                PowerBlockDialog.showPowerDialog(player, block as com.coderjoe.atlas.power.PowerBlock, powerBlockRegistry)
            }
        )

        val fluidSystem = BlockSystem<com.coderjoe.atlas.fluid.FluidBlock>(
            name = "fluid",
            registry = fluidBlockRegistry,
            factory = FluidBlockFactory,
            descriptors = fluidDescriptors(),
            showDialog = { player, block ->
                FluidBlockDialog.showFluidDialog(player, block as com.coderjoe.atlas.fluid.FluidBlock, fluidBlockRegistry)
            }
        )

        server.pluginManager.registerEvents(
            AtlasBlockListener(this, listOf(powerSystem, fluidSystem)),
            this
        )

        // Auto-save every 5 minutes (6000 ticks)
        autoSaveTask = server.scheduler.runTaskTimer(this, Runnable {
            powerBlockPersistence.save(powerBlockRegistry)
            fluidBlockPersistence.save(fluidBlockRegistry)
        }, 6000L, 6000L)

        logger.atlasInfo("Atlas plugin enabled!")
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

        logger.atlasInfo("Atlas plugin has been disabled!")
    }

    fun initPowerSystem() {
        PowerBlockFactory.registerFromDescriptors(powerDescriptors().values)
        powerBlockRegistry = PowerBlockRegistry(this)
        powerBlockPersistence = PowerBlockPersistence(this)
        powerBlockPersistence.load(powerBlockRegistry)

        logger.atlasInfo("Power system initialized with ${PowerBlockFactory.getRegisteredBlockIds().size} block types")
    }

    fun initFluidSystem() {
        FluidBlockFactory.registerFromDescriptors(fluidDescriptors().values)
        fluidBlockRegistry = FluidBlockRegistry(this)
        fluidBlockPersistence = FluidBlockPersistence(this)
        fluidBlockPersistence.load(fluidBlockRegistry)

        logger.atlasInfo("Fluid system initialized with ${FluidBlockFactory.getRegisteredBlockIds().size} block types")
    }

    private fun powerDescriptors(): Map<String, com.coderjoe.atlas.core.BlockDescriptor> {
        return listOf(
            com.coderjoe.atlas.power.block.SmallSolarPanel.descriptor,
            com.coderjoe.atlas.power.block.SmallDrill.descriptor,
            com.coderjoe.atlas.power.block.SmallBattery.descriptor,
            com.coderjoe.atlas.power.block.PowerCable.descriptor
        ).associateBy { it.baseBlockId }
    }

    private fun fluidDescriptors(): Map<String, com.coderjoe.atlas.core.BlockDescriptor> {
        return listOf(
            com.coderjoe.atlas.fluid.block.FluidPump.descriptor,
            com.coderjoe.atlas.fluid.block.FluidPipe.descriptor,
            com.coderjoe.atlas.fluid.block.FluidContainer.descriptor
        ).associateBy { it.baseBlockId }
    }
}

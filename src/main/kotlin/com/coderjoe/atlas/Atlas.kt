package com.coderjoe.atlas

import com.coderjoe.atlas.core.AtlasBlockDialog
import com.coderjoe.atlas.core.AtlasBlockListener
import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.BlockSystem
import com.coderjoe.atlas.fluid.FluidBlock
import com.coderjoe.atlas.fluid.FluidBlockDialog
import com.coderjoe.atlas.fluid.FluidBlockFactory
import com.coderjoe.atlas.fluid.FluidBlockPersistence
import com.coderjoe.atlas.fluid.FluidBlockRegistry
import com.coderjoe.atlas.fluid.block.FluidContainer
import com.coderjoe.atlas.fluid.block.FluidMerger
import com.coderjoe.atlas.fluid.block.FluidPipe
import com.coderjoe.atlas.fluid.block.FluidPump
import com.coderjoe.atlas.fluid.block.FluidSplitter
import com.coderjoe.atlas.guide.GuideBook
import com.coderjoe.atlas.guide.GuideBookListener
import com.coderjoe.atlas.power.PowerBlock
import com.coderjoe.atlas.power.PowerBlockDialog
import com.coderjoe.atlas.power.PowerBlockFactory
import com.coderjoe.atlas.power.PowerBlockPersistence
import com.coderjoe.atlas.power.PowerBlockRegistry
import com.coderjoe.atlas.power.block.LavaGenerator
import com.coderjoe.atlas.power.block.PowerCable
import com.coderjoe.atlas.power.block.PowerMerger
import com.coderjoe.atlas.power.block.PowerSplitter
import com.coderjoe.atlas.power.block.SmallBattery
import com.coderjoe.atlas.power.block.SmallSolarPanel
import com.coderjoe.atlas.transport.TransportBlock
import com.coderjoe.atlas.transport.TransportBlockDialog
import com.coderjoe.atlas.transport.TransportBlockFactory
import com.coderjoe.atlas.transport.TransportBlockPersistence
import com.coderjoe.atlas.transport.TransportBlockRegistry
import com.coderjoe.atlas.transport.block.ConveyorBelt
import com.coderjoe.atlas.utility.block.AutoSmelter
import com.coderjoe.atlas.utility.block.CobblestoneFactory
import com.coderjoe.atlas.utility.block.Crusher
import com.coderjoe.atlas.utility.block.ExperienceExtractor
import com.coderjoe.atlas.utility.block.ObsidianFactory
import com.coderjoe.atlas.utility.block.SmallDrill
import com.coderjoe.atlas.utility.block.SoftTouchDrill
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask

class Atlas : JavaPlugin() {
    private lateinit var craftEngineIntegration: CraftEngineIntegration
    private lateinit var powerBlockRegistry: PowerBlockRegistry
    private lateinit var powerBlockPersistence: PowerBlockPersistence
    private lateinit var fluidBlockRegistry: FluidBlockRegistry
    private lateinit var fluidBlockPersistence: FluidBlockPersistence
    private lateinit var transportBlockRegistry: TransportBlockRegistry
    private lateinit var transportBlockPersistence: TransportBlockPersistence
    private var autoSaveTask: BukkitTask? = null

    override fun onEnable() {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }

        AtlasConfig.load(this)

        craftEngineIntegration = CraftEngineIntegration(this)
        craftEngineIntegration.initialize()

        server.pluginManager.registerEvents(PlayerJoinListener(), this)

        AtlasBlockDialog.init(this)

        initPowerSystem()
        initFluidSystem()
        initTransportSystem()

        // Register unified listener
        val powerSystem =
            BlockSystem<PowerBlock>(
                name = "power",
                registry = powerBlockRegistry,
                factory = PowerBlockFactory,
                descriptors = powerDescriptors(),
                showDialog = { player, block ->
                    PowerBlockDialog.showPowerDialog(player, block as PowerBlock, powerBlockRegistry)
                },
            )

        val fluidSystem =
            BlockSystem<FluidBlock>(
                name = "fluid",
                registry = fluidBlockRegistry,
                factory = FluidBlockFactory,
                descriptors = fluidDescriptors(),
                showDialog = { player, block ->
                    FluidBlockDialog.showFluidDialog(player, block as FluidBlock, fluidBlockRegistry)
                },
            )

        val transportSystem =
            BlockSystem<TransportBlock>(
                name = "transport",
                registry = transportBlockRegistry,
                factory = TransportBlockFactory,
                descriptors = transportDescriptors(),
                showDialog = { player, block ->
                    TransportBlockDialog.showTransportDialog(
                        player,
                        block as TransportBlock,
                        transportBlockRegistry,
                    )
                },
            )

        server.pluginManager.registerEvents(
            AtlasBlockListener(this, listOf(powerSystem, fluidSystem, transportSystem)),
            this,
        )

        val guideBookListener = GuideBookListener(this)
        server.pluginManager.registerEvents(guideBookListener, this)
        server.addRecipe(GuideBook.createRecipe(this))

        // Auto-save every 5 minutes (6000 ticks)
        autoSaveTask =
            server.scheduler.runTaskTimer(
                this,
                Runnable {
                    powerBlockPersistence.save(powerBlockRegistry)
                    fluidBlockPersistence.save(fluidBlockRegistry)
                    transportBlockPersistence.save(transportBlockRegistry)
                },
                6000L, 6000L,
            )

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

        if (::transportBlockPersistence.isInitialized && ::transportBlockRegistry.isInitialized) {
            transportBlockPersistence.save(transportBlockRegistry)
        }

        AtlasBlockDialog.cleanup()

        if (::powerBlockRegistry.isInitialized) {
            powerBlockRegistry.stopAll()
        }

        if (::fluidBlockRegistry.isInitialized) {
            fluidBlockRegistry.stopAll()
        }

        if (::transportBlockRegistry.isInitialized) {
            transportBlockRegistry.stopAll()
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

    fun initTransportSystem() {
        TransportBlockFactory.registerFromDescriptors(transportDescriptors().values)
        transportBlockRegistry = TransportBlockRegistry(this)
        transportBlockPersistence = TransportBlockPersistence(this)
        transportBlockPersistence.load(transportBlockRegistry)

        logger.atlasInfo("Transport system initialized with ${TransportBlockFactory.getRegisteredBlockIds().size} block types")
    }

    private fun transportDescriptors(): Map<String, BlockDescriptor> {
        return listOf(
            ConveyorBelt.descriptor,
        ).associateBy { it.baseBlockId }
    }

    private fun powerDescriptors(): Map<String, BlockDescriptor> {
        return listOf(
            SmallSolarPanel.descriptor,
            SmallDrill.descriptor,
            SmallBattery.descriptor,
            PowerCable.descriptor,
            LavaGenerator.descriptor,
            AutoSmelter.descriptor,
            PowerSplitter.descriptor,
            CobblestoneFactory.descriptor,
            ObsidianFactory.descriptor,
            Crusher.descriptor,
            PowerMerger.descriptor,
            SoftTouchDrill.descriptor,
            ExperienceExtractor.descriptor,
        ).associateBy { it.baseBlockId }
    }

    private fun fluidDescriptors(): Map<String, BlockDescriptor> {
        return listOf(
            FluidPump.descriptor,
            FluidPipe.descriptor,
            FluidContainer.descriptor,
            FluidMerger.descriptor,
            FluidSplitter.descriptor,
        ).associateBy { it.baseBlockId }
    }
}

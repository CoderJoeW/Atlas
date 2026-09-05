package com.coderjoe.atlas

import com.coderjoe.atlas.core.AtlasBlockDialog
import com.coderjoe.atlas.core.AtlasBlockListener
import com.coderjoe.atlas.core.AtlasSubsystem
import com.coderjoe.atlas.core.AtlasWrench
import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.BlockSystem
import com.coderjoe.atlas.fluid.FluidBlock
import com.coderjoe.atlas.fluid.FluidBlockDialog
import com.coderjoe.atlas.fluid.FluidBlockFactory
import com.coderjoe.atlas.fluid.FluidBlockPersistence
import com.coderjoe.atlas.fluid.FluidBlockRegistry
import com.coderjoe.atlas.fluid.block.FluidContainer
import com.coderjoe.atlas.fluid.block.FluidPipe
import com.coderjoe.atlas.fluid.block.FluidPump
import com.coderjoe.atlas.guide.GuideBook
import com.coderjoe.atlas.guide.GuideBookListener
import com.coderjoe.atlas.power.PowerBlock
import com.coderjoe.atlas.power.PowerBlockDialog
import com.coderjoe.atlas.power.PowerBlockFactory
import com.coderjoe.atlas.power.PowerBlockPersistence
import com.coderjoe.atlas.power.PowerBlockRegistry
import com.coderjoe.atlas.power.block.LavaGenerator
import com.coderjoe.atlas.power.block.PowerCable
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
import com.coderjoe.atlas.utility.block.ObsidianFactory
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask

class Atlas : JavaPlugin() {
    private lateinit var craftEngineIntegration: CraftEngineIntegration
    private lateinit var powerSubsystem: AtlasSubsystem<PowerBlock>
    private lateinit var fluidSubsystem: AtlasSubsystem<FluidBlock>
    private lateinit var transportSubsystem: AtlasSubsystem<TransportBlock>
    private var autoSaveTask: BukkitTask? = null

    private val subsystems: List<AtlasSubsystem<*>>
        get() = listOf(powerSubsystem, fluidSubsystem, transportSubsystem)

    override fun onEnable() {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }

        AtlasConfig.load(this)

        craftEngineIntegration = CraftEngineIntegration(this)
        craftEngineIntegration.initialize()

        server.pluginManager.registerEvents(PlayerJoinListener(), this)

        AtlasBlockDialog.init(this)

        powerSubsystem =
            AtlasSubsystem(
                name = "power",
                registry = PowerBlockRegistry(this),
                factory = PowerBlockFactory,
                descriptors = powerDescriptors(),
                persistence = PowerBlockPersistence(this),
                plugin = this,
            )
        fluidSubsystem =
            AtlasSubsystem(
                name = "fluid",
                registry = FluidBlockRegistry(this),
                factory = FluidBlockFactory,
                descriptors = fluidDescriptors(),
                persistence = FluidBlockPersistence(this),
                plugin = this,
            )
        transportSubsystem =
            AtlasSubsystem(
                name = "transport",
                registry = TransportBlockRegistry(this),
                factory = TransportBlockFactory,
                descriptors = transportDescriptors(),
                persistence = TransportBlockPersistence(this),
                plugin = this,
            )
        subsystems.forEach { it.init() }

        // Register unified listener
        val powerSystem =
            BlockSystem<PowerBlock>(
                name = "power",
                registry = powerSubsystem.registry,
                factory = PowerBlockFactory,
                descriptors = powerSubsystem.descriptors,
                showDialog = { player, block ->
                    PowerBlockDialog.showPowerDialog(
                        player,
                        block as PowerBlock,
                        powerSubsystem.registry,
                        powerSubsystem.descriptors,
                    )
                },
            )

        val fluidSystem =
            BlockSystem<FluidBlock>(
                name = "fluid",
                registry = fluidSubsystem.registry,
                factory = FluidBlockFactory,
                descriptors = fluidSubsystem.descriptors,
                showDialog = { player, block ->
                    FluidBlockDialog.showFluidDialog(
                        player,
                        block as FluidBlock,
                        fluidSubsystem.registry,
                        fluidSubsystem.descriptors,
                    )
                },
            )

        val transportSystem =
            BlockSystem<TransportBlock>(
                name = "transport",
                registry = transportSubsystem.registry,
                factory = TransportBlockFactory,
                descriptors = transportSubsystem.descriptors,
                showDialog = { player, block ->
                    TransportBlockDialog.showTransportDialog(
                        player,
                        block as TransportBlock,
                        transportSubsystem.registry,
                        transportSubsystem.descriptors,
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
        server.addRecipe(AtlasWrench.createRecipe(this))

        // Auto-save every 5 minutes (6000 ticks)
        autoSaveTask =
            server.scheduler.runTaskTimer(
                this,
                Runnable { subsystems.forEach { it.save() } },
                6000L, 6000L,
            )

        logger.atlasInfo("Atlas plugin enabled!")
    }

    override fun onDisable() {
        autoSaveTask?.cancel()

        initializedSubsystems().forEach { it.save() }

        AtlasBlockDialog.cleanup()

        initializedSubsystems().forEach { it.stop() }

        logger.atlasInfo("Atlas plugin has been disabled!")
    }

    private fun initializedSubsystems(): List<AtlasSubsystem<*>> {
        val result = mutableListOf<AtlasSubsystem<*>>()
        if (::powerSubsystem.isInitialized) result.add(powerSubsystem)
        if (::fluidSubsystem.isInitialized) result.add(fluidSubsystem)
        if (::transportSubsystem.isInitialized) result.add(transportSubsystem)
        return result
    }

    private fun transportDescriptors(): Map<String, BlockDescriptor> {
        return listOf(
            ConveyorBelt.descriptor,
        ).associateBy { it.baseBlockId }
    }

    private fun powerDescriptors(): Map<String, BlockDescriptor> {
        return listOf(
            SmallSolarPanel.descriptor,
            SmallBattery.descriptor,
            PowerCable.descriptor,
            LavaGenerator.descriptor,
            AutoSmelter.descriptor,
            CobblestoneFactory.descriptor,
            ObsidianFactory.descriptor,
            Crusher.descriptor,
        ).associateBy { it.baseBlockId }
    }

    private fun fluidDescriptors(): Map<String, BlockDescriptor> {
        return listOf(
            FluidPump.descriptor,
            FluidPipe.descriptor,
            FluidContainer.descriptor,
        ).associateBy { it.baseBlockId }
    }
}

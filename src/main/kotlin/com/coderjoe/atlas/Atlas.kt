package com.coderjoe.atlas

import com.coderjoe.atlas.block.AtlasSubsystem
import com.coderjoe.atlas.block.BlockDescriptor
import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.block.BlockSystem
import com.coderjoe.atlas.block.fluid.FluidBlockFactory
import com.coderjoe.atlas.block.fluid.block.FluidContainer
import com.coderjoe.atlas.block.fluid.block.FluidPipe
import com.coderjoe.atlas.block.fluid.block.FluidPump
import com.coderjoe.atlas.block.power.LavaGenerator
import com.coderjoe.atlas.block.power.PowerBlockFactory
import com.coderjoe.atlas.block.power.PowerCable
import com.coderjoe.atlas.block.power.SmallBattery
import com.coderjoe.atlas.block.power.SmallSolarPanel
import com.coderjoe.atlas.block.power.factory.CobblestoneFactory
import com.coderjoe.atlas.block.power.factory.ObsidianFactory
import com.coderjoe.atlas.block.power.mine.CoalMine
import com.coderjoe.atlas.block.power.mine.DiamondMine
import com.coderjoe.atlas.block.power.mine.EmeraldMine
import com.coderjoe.atlas.block.power.mine.GoldMine
import com.coderjoe.atlas.block.power.mine.IronMine
import com.coderjoe.atlas.block.power.mine.NetheriteMine
import com.coderjoe.atlas.block.power.mine.RedstoneMine
import com.coderjoe.atlas.block.transport.TransportBlockFactory
import com.coderjoe.atlas.block.transport.block.ConveyorBelt
import com.coderjoe.atlas.craftengine.CraftEngineIntegration
import com.coderjoe.atlas.data.FluidBlockPersistence
import com.coderjoe.atlas.data.PowerBlockPersistence
import com.coderjoe.atlas.data.TransportBlockPersistence
import com.coderjoe.atlas.dialog.AtlasBlockDialog
import com.coderjoe.atlas.dialog.BlockInspectorDialog
import com.coderjoe.atlas.item.AtlasWrench
import com.coderjoe.atlas.item.GuideBook
import com.coderjoe.atlas.listener.AtlasBlockListener
import com.coderjoe.atlas.listener.GuideBookListener
import com.coderjoe.atlas.listener.PlayerJoinListener
import com.coderjoe.atlas.util.AtlasConfig
import com.coderjoe.atlas.util.atlasInfo
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask

class Atlas : JavaPlugin() {
    private lateinit var craftEngineIntegration: CraftEngineIntegration
    private lateinit var registry: BlockRegistry
    private lateinit var powerSubsystem: AtlasSubsystem
    private lateinit var fluidSubsystem: AtlasSubsystem
    private lateinit var transportSubsystem: AtlasSubsystem
    private var autoSaveTask: BukkitTask? = null

    private val subsystems: List<AtlasSubsystem>
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

        // One index for every block, whatever system it belongs to, so a lookup can no longer miss
        // a neighbour because it was filed somewhere else. The three subsystems still own a save
        // file and a factory each until steps 2.6 and 2.7 retire them.
        registry = BlockRegistry(this)

        powerSubsystem =
            AtlasSubsystem(
                name = "power",
                registry = registry,
                factory = PowerBlockFactory,
                descriptors = powerDescriptors(),
                persistence = PowerBlockPersistence(this),
                plugin = this,
            )
        fluidSubsystem =
            AtlasSubsystem(
                name = "fluid",
                registry = registry,
                factory = FluidBlockFactory,
                descriptors = fluidDescriptors(),
                persistence = FluidBlockPersistence(this),
                plugin = this,
            )
        transportSubsystem =
            AtlasSubsystem(
                name = "transport",
                registry = registry,
                factory = TransportBlockFactory,
                descriptors = transportDescriptors(),
                persistence = TransportBlockPersistence(this),
                plugin = this,
            )
        subsystems.forEach { it.init() }

        // Register unified listener
        val powerSystem =
            BlockSystem(
                name = "power",
                registry = registry,
                factory = PowerBlockFactory,
                descriptors = powerSubsystem.descriptors,
            )

        val fluidSystem =
            BlockSystem(
                name = "fluid",
                registry = registry,
                factory = FluidBlockFactory,
                descriptors = fluidSubsystem.descriptors,
            )

        val transportSystem =
            BlockSystem(
                name = "transport",
                registry = registry,
                factory = TransportBlockFactory,
                descriptors = transportSubsystem.descriptors,
            )

        server.pluginManager.registerEvents(
            AtlasBlockListener(this, registry, listOf(powerSystem, fluidSystem, transportSystem)) { player, block ->
                BlockInspectorDialog.show(player, block, registry, AtlasBlockTypes.catalog)
            },
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

    private fun initializedSubsystems(): List<AtlasSubsystem> {
        val result = mutableListOf<AtlasSubsystem>()
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
            CobblestoneFactory.descriptor,
            ObsidianFactory.descriptor,
            CoalMine.descriptor,
            IronMine.descriptor,
            RedstoneMine.descriptor,
            GoldMine.descriptor,
            EmeraldMine.descriptor,
            DiamondMine.descriptor,
            NetheriteMine.descriptor,
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

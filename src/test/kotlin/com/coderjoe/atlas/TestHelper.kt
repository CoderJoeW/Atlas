package com.coderjoe.atlas

import com.coderjoe.atlas.core.AtlasBlock
import com.coderjoe.atlas.core.BlockRegistry
import com.coderjoe.atlas.fluid.FluidBlock
import com.coderjoe.atlas.fluid.FluidBlockFactory
import com.coderjoe.atlas.fluid.FluidBlockRegistry
import com.coderjoe.atlas.fluid.block.FluidContainer
import com.coderjoe.atlas.fluid.block.FluidMerger
import com.coderjoe.atlas.fluid.block.FluidPipe
import com.coderjoe.atlas.fluid.block.FluidPump
import com.coderjoe.atlas.power.PowerBlock
import com.coderjoe.atlas.power.PowerBlockFactory
import com.coderjoe.atlas.power.PowerBlockRegistry
import com.coderjoe.atlas.power.block.LavaGenerator
import com.coderjoe.atlas.power.block.PowerCable
import com.coderjoe.atlas.power.block.PowerMerger
import com.coderjoe.atlas.power.block.PowerSplitter
import com.coderjoe.atlas.power.block.SmallBattery
import com.coderjoe.atlas.power.block.SmallSolarPanel
import com.coderjoe.atlas.transport.TransportBlock
import com.coderjoe.atlas.transport.TransportBlockFactory
import com.coderjoe.atlas.transport.TransportBlockRegistry
import com.coderjoe.atlas.transport.block.ConveyorBelt
import com.coderjoe.atlas.utility.block.AutoSmelter
import com.coderjoe.atlas.utility.block.CobblestoneFactory
import com.coderjoe.atlas.utility.block.ObsidianFactory
import com.coderjoe.atlas.utility.block.SmallDrill
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.bukkit.Location
import org.bukkit.Server
import org.bukkit.World
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitScheduler
import org.bukkit.scheduler.BukkitTask
import java.io.File
import java.util.logging.Logger

object TestHelper {
    lateinit var mockPlugin: Atlas
    lateinit var mockServer: Server
    lateinit var mockWorld: World
    lateinit var mockScheduler: BukkitScheduler
    lateinit var dataFolder: File

    fun setup() {
        mockPlugin = mockk<Atlas>(relaxed = true)
        mockServer = mockk<Server>(relaxed = true)
        mockWorld = mockk<World>(relaxed = true)
        mockScheduler = mockk<BukkitScheduler>(relaxed = true)

        dataFolder = File(System.getProperty("java.io.tmpdir"), "atlas-test-${System.nanoTime()}")
        dataFolder.mkdirs()

        // Set test plugin hook on base class (avoids JavaPlugin.getPlugin() call)
        AtlasBlock.testPlugin = mockPlugin

        every { mockPlugin.server } returns mockServer
        every { mockPlugin.logger } returns Logger.getLogger("TestAtlas")
        every { mockPlugin.dataFolder } returns dataFolder
        every { mockServer.getWorld("world") } returns mockWorld
        every { mockServer.getWorld(match<String> { it != "world" }) } returns null
        every { mockServer.scheduler } returns mockScheduler
        every { mockWorld.name } returns "world"
        every { mockWorld.time } returns 6000L
        every { mockWorld.minHeight } returns -64

        val mockTask = mockk<BukkitTask>(relaxed = true)
        every { mockScheduler.runTask(any<JavaPlugin>(), any<Runnable>()) } returns mockTask
        every { mockScheduler.runTaskTimer(any<JavaPlugin>(), any<Runnable>(), any(), any()) } returns mockTask

        clearRegistries()
        clearFactories()
    }

    fun teardown() {
        unmockkAll()
        AtlasBlock.testPlugin = null
        clearRegistries()
        clearFactories()
        dataFolder.deleteRecursively()
    }

    fun createLocation(
        x: Double = 0.0,
        y: Double = 64.0,
        z: Double = 0.0,
        world: World? = null,
    ): Location {
        return Location(world ?: mockWorld, x, y, z)
    }

    fun PowerBlock.callPowerUpdate() {
        val method = PowerBlock::class.java.getDeclaredMethod("powerUpdate")
        method.isAccessible = true
        method.invoke(this)
    }

    fun FluidBlock.callFluidUpdate() {
        val method = FluidBlock::class.java.getDeclaredMethod("fluidUpdate")
        method.isAccessible = true
        method.invoke(this)
    }

    fun TransportBlock.callTransportUpdate() {
        val method = TransportBlock::class.java.getDeclaredMethod("transportUpdate")
        method.isAccessible = true
        method.invoke(this)
    }

    fun addToRegistry(
        registry: PowerBlockRegistry,
        block: PowerBlock,
        blockId: String,
    ) {
        val blocksField = BlockRegistry::class.java.getDeclaredField("blocks")
        blocksField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val blocks = blocksField.get(registry) as java.util.concurrent.ConcurrentHashMap<String, PowerBlock>

        val blockIdsField = BlockRegistry::class.java.getDeclaredField("blockIds")
        blockIdsField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val blockIds = blockIdsField.get(registry) as java.util.concurrent.ConcurrentHashMap<String, String>

        val key = PowerBlockRegistry.locationKey(block.location)
        blocks[key] = block
        blockIds[key] = blockId
    }

    fun addToRegistry(
        registry: TransportBlockRegistry,
        block: TransportBlock,
        blockId: String,
    ) {
        val blocksField = BlockRegistry::class.java.getDeclaredField("blocks")
        blocksField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val blocks = blocksField.get(registry) as java.util.concurrent.ConcurrentHashMap<String, TransportBlock>

        val blockIdsField = BlockRegistry::class.java.getDeclaredField("blockIds")
        blockIdsField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val blockIds = blockIdsField.get(registry) as java.util.concurrent.ConcurrentHashMap<String, String>

        val key = TransportBlockRegistry.locationKey(block.location)
        blocks[key] = block
        blockIds[key] = blockId
    }

    fun addToRegistry(
        registry: FluidBlockRegistry,
        block: FluidBlock,
        blockId: String,
    ) {
        val blocksField = BlockRegistry::class.java.getDeclaredField("blocks")
        blocksField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val blocks = blocksField.get(registry) as java.util.concurrent.ConcurrentHashMap<String, FluidBlock>

        val blockIdsField = BlockRegistry::class.java.getDeclaredField("blockIds")
        blockIdsField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val blockIds = blockIdsField.get(registry) as java.util.concurrent.ConcurrentHashMap<String, String>

        val key = FluidBlockRegistry.locationKey(block.location)
        blocks[key] = block
        blockIds[key] = blockId
    }

    private fun clearRegistries() {
        try {
            val instanceField = PowerBlockRegistry.Companion::class.java.getDeclaredField("instance")
            instanceField.isAccessible = true
            instanceField.set(PowerBlockRegistry.Companion, null)
        } catch (_: Exception) {
        }

        try {
            val instanceField = FluidBlockRegistry.Companion::class.java.getDeclaredField("instance")
            instanceField.isAccessible = true
            instanceField.set(FluidBlockRegistry.Companion, null)
        } catch (_: Exception) {
        }

        try {
            val instanceField = TransportBlockRegistry.Companion::class.java.getDeclaredField("instance")
            instanceField.isAccessible = true
            instanceField.set(TransportBlockRegistry.Companion, null)
        } catch (_: Exception) {
        }
    }

    fun initPowerFactory() {
        PowerBlockFactory.registerFromDescriptors(
            listOf(
                SmallSolarPanel.descriptor, SmallDrill.descriptor,
                SmallBattery.descriptor, PowerCable.descriptor,
                LavaGenerator.descriptor, AutoSmelter.descriptor,
                PowerSplitter.descriptor, CobblestoneFactory.descriptor,
                ObsidianFactory.descriptor, PowerMerger.descriptor,
            ),
        )
    }

    fun initFluidFactory() {
        FluidBlockFactory.registerFromDescriptors(
            listOf(
                FluidPump.descriptor,
                FluidPipe.descriptor,
                FluidContainer.descriptor,
                FluidMerger.descriptor,
            ),
        )
    }

    fun initTransportFactory() {
        TransportBlockFactory.registerFromDescriptors(
            listOf(
                ConveyorBelt.descriptor,
            ),
        )
    }

    private fun clearFactories() {
        PowerBlockFactory.clear()
        FluidBlockFactory.clear()
        TransportBlockFactory.clear()
    }
}

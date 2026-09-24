package com.coderjoe.atlas.testing

import com.coderjoe.atlas.Atlas
import com.coderjoe.atlas.block.AtlasBlock
import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.block.fluid.FluidBlock
import com.coderjoe.atlas.block.fluid.FluidBlockFactory
import com.coderjoe.atlas.block.fluid.block.FluidContainer
import com.coderjoe.atlas.block.fluid.block.FluidPipe
import com.coderjoe.atlas.block.fluid.block.FluidPump
import com.coderjoe.atlas.block.power.LavaGenerator
import com.coderjoe.atlas.block.power.PowerBlock
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
import com.coderjoe.atlas.block.transport.TransportBlock
import com.coderjoe.atlas.block.transport.TransportBlockFactory
import com.coderjoe.atlas.block.transport.block.ConveyorBelt
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

        BlockRegistry.clearActive()
        clearFactories()
    }

    fun teardown() {
        unmockkAll()
        AtlasBlock.testPlugin = null
        BlockRegistry.clearActive()
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

    fun AtlasBlock.callSpawnEffects() {
        val method = AtlasBlock::class.java.getDeclaredMethod("spawnEffects")
        method.isAccessible = true
        method.invoke(this)
    }

    /**
     * Puts [block] straight into the index without starting it, so a test can lay out a
     * neighbourhood without every block scheduling a task.
     */
    fun addToRegistry(
        registry: BlockRegistry,
        block: AtlasBlock,
        blockId: String,
    ) {
        val blocksField = BlockRegistry::class.java.getDeclaredField("blocks")
        blocksField.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        val blocks = blocksField.get(registry) as java.util.concurrent.ConcurrentHashMap<String, AtlasBlock>

        val blockIdsField = BlockRegistry::class.java.getDeclaredField("blockIds")
        blockIdsField.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        val blockIds = blockIdsField.get(registry) as java.util.concurrent.ConcurrentHashMap<String, String>

        val key = BlockRegistry.locationKey(block.location)
        blocks[key] = block
        blockIds[key] = blockId
    }

    fun initPowerFactory() {
        PowerBlockFactory.registerFromDescriptors(
            listOf(
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
            ),
        )
    }

    fun initFluidFactory() {
        FluidBlockFactory.registerFromDescriptors(
            listOf(
                FluidPump.descriptor,
                FluidPipe.descriptor,
                FluidContainer.descriptor,
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

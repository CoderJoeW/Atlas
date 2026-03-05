package com.coderjoe.atlas

import com.coderjoe.atlas.fluid.FluidBlock
import com.coderjoe.atlas.fluid.FluidBlockFactory
import com.coderjoe.atlas.fluid.FluidBlockRegistry
import com.coderjoe.atlas.power.PowerBlock
import com.coderjoe.atlas.power.PowerBlockFactory
import com.coderjoe.atlas.power.PowerBlockRegistry
import io.mockk.*
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

        // Set test plugin hooks on base classes (avoids JavaPlugin.getPlugin() call)
        PowerBlock.testPlugin = mockPlugin
        FluidBlock.testPlugin = mockPlugin

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
        PowerBlock.testPlugin = null
        FluidBlock.testPlugin = null
        clearRegistries()
        clearFactories()
        dataFolder.deleteRecursively()
    }

    fun createLocation(x: Double = 0.0, y: Double = 64.0, z: Double = 0.0, world: World? = null): Location {
        return Location(world ?: mockWorld, x, y, z)
    }

    fun PowerBlock.callPowerUpdate() {
        val method = this::class.java.getDeclaredMethod("powerUpdate")
        method.isAccessible = true
        method.invoke(this)
    }

    fun FluidBlock.callFluidUpdate() {
        val method = this::class.java.getDeclaredMethod("fluidUpdate")
        method.isAccessible = true
        method.invoke(this)
    }

    fun addToRegistry(registry: PowerBlockRegistry, block: PowerBlock, blockId: String) {
        val powerBlocksField = PowerBlockRegistry::class.java.getDeclaredField("powerBlocks")
        powerBlocksField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val powerBlocks = powerBlocksField.get(registry) as java.util.concurrent.ConcurrentHashMap<String, PowerBlock>

        val blockIdsField = PowerBlockRegistry::class.java.getDeclaredField("blockIds")
        blockIdsField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val blockIds = blockIdsField.get(registry) as java.util.concurrent.ConcurrentHashMap<String, String>

        val key = PowerBlockRegistry.locationKey(block.location)
        powerBlocks[key] = block
        blockIds[key] = blockId
    }

    fun addToRegistry(registry: FluidBlockRegistry, block: FluidBlock, blockId: String) {
        val fluidBlocksField = FluidBlockRegistry::class.java.getDeclaredField("fluidBlocks")
        fluidBlocksField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val fluidBlocks = fluidBlocksField.get(registry) as java.util.concurrent.ConcurrentHashMap<String, FluidBlock>

        val blockIdsField = FluidBlockRegistry::class.java.getDeclaredField("blockIds")
        blockIdsField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val blockIds = blockIdsField.get(registry) as java.util.concurrent.ConcurrentHashMap<String, String>

        val key = FluidBlockRegistry.locationKey(block.location)
        fluidBlocks[key] = block
        blockIds[key] = blockId
    }

    private fun clearRegistries() {
        try {
            val instanceField = PowerBlockRegistry.Companion::class.java.getDeclaredField("instance")
            instanceField.isAccessible = true
            instanceField.set(PowerBlockRegistry.Companion, null)
        } catch (_: Exception) {}

        try {
            val instanceField = FluidBlockRegistry.Companion::class.java.getDeclaredField("instance")
            instanceField.isAccessible = true
            instanceField.set(FluidBlockRegistry.Companion, null)
        } catch (_: Exception) {}
    }

    private fun clearFactories() {
        try {
            val field = PowerBlockFactory::class.java.getDeclaredField("blockConstructors")
            field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            (field.get(PowerBlockFactory) as MutableMap<*, *>).clear()
        } catch (_: Exception) {}

        try {
            val field = FluidBlockFactory::class.java.getDeclaredField("blockConstructors")
            field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            (field.get(FluidBlockFactory) as MutableMap<*, *>).clear()
        } catch (_: Exception) {}
    }
}

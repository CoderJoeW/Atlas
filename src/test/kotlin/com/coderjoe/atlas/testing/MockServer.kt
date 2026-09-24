package com.coderjoe.atlas.testing

import com.coderjoe.atlas.Atlas
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

/**
 * A relaxed Bukkit stand-in: a plugin whose server has one world called "world" and a scheduler
 * that accepts every task without running it.
 *
 * Nothing here knows about Atlas blocks. A test builds a [com.coderjoe.atlas.block.BlockRegistry]
 * on [plugin] and places blocks into it with the fixtures in [Blocks].
 */
object MockServer {
    lateinit var plugin: Atlas
    lateinit var server: Server
    lateinit var world: World
    lateinit var scheduler: BukkitScheduler
    lateinit var dataFolder: File

    fun setup() {
        plugin = mockk<Atlas>(relaxed = true)
        server = mockk<Server>(relaxed = true)
        world = mockk<World>(relaxed = true)
        scheduler = mockk<BukkitScheduler>(relaxed = true)

        dataFolder = File(System.getProperty("java.io.tmpdir"), "atlas-test-${System.nanoTime()}")
        dataFolder.mkdirs()

        every { plugin.server } returns server
        every { plugin.logger } returns Logger.getLogger("TestAtlas")
        every { plugin.dataFolder } returns dataFolder
        every { server.getWorld("world") } returns world
        every { server.getWorld(match<String> { it != "world" }) } returns null
        every { server.scheduler } returns scheduler
        every { world.name } returns "world"
        every { world.time } returns 6000L
        every { world.minHeight } returns -64

        val task = mockk<BukkitTask>(relaxed = true)
        every { scheduler.runTask(any<JavaPlugin>(), any<Runnable>()) } returns task
        every { scheduler.runTaskTimer(any<JavaPlugin>(), any<Runnable>(), any(), any()) } returns task

        Blocks.clearFactories()
    }

    fun teardown() {
        unmockkAll()
        Blocks.clearFactories()
        dataFolder.deleteRecursively()
    }

    fun createLocation(
        x: Double = 0.0,
        y: Double = 64.0,
        z: Double = 0.0,
        world: World? = null,
    ): Location {
        return Location(world ?: this.world, x, y, z)
    }
}

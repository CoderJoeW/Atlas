package com.coderjoe.atlas.block

import com.coderjoe.atlas.craftengine.CraftEngineHelper
import com.coderjoe.atlas.testing.MockServer
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.verify
import org.bukkit.Location
import org.bukkit.plugin.java.JavaPlugin
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BlockTickerTest {
    private lateinit var registry: BlockRegistry

    private class Probe(
        location: Location,
        override val updateIntervalTicks: Long = 20L,
        override val effectIntervalTicks: Long = 0L,
        private val failing: Boolean = false,
    ) : AtlasBlock(location) {
        val updatedAt = mutableListOf<Long>()
        val effectsAt = mutableListOf<Long>()
        var state = "atlas:probe_idle"
        lateinit var ticker: BlockTicker

        override fun blockUpdate() {
            updatedAt += ticker.currentTick
            if (failing) error("boom")
            state = "atlas:probe_working"
        }

        override fun spawnEffects() {
            effectsAt += ticker.currentTick
        }

        override fun getVisualStateBlockId(): String = state
    }

    @BeforeEach
    fun setup() {
        MockServer.setup()
        mockkObject(CraftEngineHelper)
        every { CraftEngineHelper.placeState(any(), any()) } just runs
        registry = BlockRegistry(MockServer.plugin)
    }

    @AfterEach
    fun teardown() {
        MockServer.teardown()
    }

    private fun probe(
        x: Int,
        z: Int = 0,
        interval: Long = 20L,
        effects: Long = 0L,
        failing: Boolean = false,
    ): Probe =
        Probe(MockServer.createLocation(x.toDouble(), 64.0, z.toDouble()), interval, effects, failing).also {
            it.ticker = registry.ticker
            registry.register(it, "atlas:probe")
        }

    private fun runTicks(count: Int) = repeat(count) { registry.ticker.tick() }

    @Test
    fun `a block updates exactly once per interval, evenly spaced`() {
        val block = probe(3, interval = 5L)

        runTicks(50)

        assertEquals(10, block.updatedAt.size)
        assertTrue(block.updatedAt.zipWithNext().all { (a, b) -> b - a == 5L }, "updates at ${block.updatedAt}")
    }

    @Test
    fun `blocks on the same interval are spread across its ticks`() {
        val blocks = (0 until 10).flatMap { x -> (0 until 10).map { z -> probe(x, z) } }

        runTicks(20)

        assertTrue(blocks.all { it.updatedAt.size == 1 }, "every block runs once per interval")
        val perTick = blocks.groupingBy { it.updatedAt.single() }.eachCount()
        assertTrue(perTick.size >= 15, "100 blocks should land on most of the 20 slots, got ${perTick.size}")
        assertTrue(perTick.values.max() <= 15, "no single tick should carry the lane, got ${perTick.values.max()}")
    }

    @Test
    fun `a block lands on the same slot every time it is scheduled`() {
        val first = probe(7, 11)
        runTicks(20)
        registry.unregister(first.location)

        val again = probe(7, 11)
        runTicks(20)

        assertEquals(first.updatedAt.single() % 20, again.updatedAt.single() % 20)
    }

    @Test
    fun `effects run on their own interval and not at all when it is zero`() {
        val ambient = probe(1, interval = 20L, effects = 4L)
        val quiet = probe(2, interval = 20L)

        runTicks(40)

        assertEquals(2, ambient.updatedAt.size)
        assertEquals(10, ambient.effectsAt.size)
        assertTrue(quiet.effectsAt.isEmpty())
    }

    @Test
    fun `one block throwing does not stop the others or itself`() {
        val broken = probe(0, interval = 1L, failing = true)
        val healthy = probe(1, interval = 1L)

        runTicks(3)

        assertEquals(3, broken.updatedAt.size, "the failing block is still ticked")
        assertEquals(3, healthy.updatedAt.size)
    }

    @Test
    fun `the visual state is refreshed after each update`() {
        val block = probe(0, interval = 1L)

        runTicks(1)

        verify { CraftEngineHelper.placeState(block.location, "atlas:probe_working") }
    }

    @Test
    fun `only registered blocks are ticked, and unregistering or stopping takes them off`() {
        val tracked = Probe(MockServer.createLocation(5.0), 1L).also { it.ticker = registry.ticker }
        registry.track(tracked, "atlas:probe")
        val registered = probe(6, interval = 1L)
        val stopped = probe(7, interval = 1L)

        runTicks(2)
        registry.unregister(stopped.location)
        runTicks(2)

        assertTrue(tracked.updatedAt.isEmpty(), "track() alone leaves the block for a test to drive")
        assertEquals(4, registered.updatedAt.size)
        assertEquals(2, stopped.updatedAt.size)

        registry.stopAll()
        runTicks(2)
        assertEquals(4, registered.updatedAt.size)
    }

    @Test
    fun `a block replaced at the same location stops ticking`() {
        val old = probe(0, interval = 1L)
        val replacement = probe(0, interval = 1L)

        runTicks(2)

        assertTrue(old.updatedAt.isEmpty())
        assertEquals(2, replacement.updatedAt.size)
    }

    @Test
    fun `systems run once per interval`() {
        var runs = 0
        registry.ticker.addSystem("counter", 10L) { runs++ }

        runTicks(100)

        assertEquals(10, runs)
    }

    @Test
    fun `scheduled blocks are indexed by chunk`() {
        val near = probe(1, 1)
        val sameChunk = probe(15, 15)
        val nextChunk = probe(16, 0)

        assertEquals(setOf(near, sameChunk), registry.ticker.scheduledIn(BlockTicker.ChunkKey("world", 0, 0)))
        assertEquals(setOf(nextChunk), registry.ticker.scheduledIn(BlockTicker.ChunkKey("world", 1, 0)))

        registry.unregister(nextChunk.location)
        assertTrue(registry.ticker.scheduledIn(BlockTicker.ChunkKey("world", 1, 0)).isEmpty())
        assertFalse(registry.ticker.isScheduled(nextChunk))
    }

    @Test
    fun `the whole registry shares one scheduler task`() {
        repeat(5) { probe(it) }

        verify(exactly = 1) { MockServer.scheduler.runTaskTimer(any<JavaPlugin>(), any<Runnable>(), 1L, 1L) }
    }
}

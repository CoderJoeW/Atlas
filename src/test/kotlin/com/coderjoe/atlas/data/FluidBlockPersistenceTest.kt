package com.coderjoe.atlas.data

import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.block.capability.FluidType
import com.coderjoe.atlas.block.fluid.FluidPipe
import com.coderjoe.atlas.block.fluid.FluidPump
import com.coderjoe.atlas.block.power.SmallBattery
import com.coderjoe.atlas.testing.Blocks
import com.coderjoe.atlas.testing.MockServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class FluidBlockPersistenceTest {
    private lateinit var registry: BlockRegistry
    private lateinit var persistence: FluidBlockPersistence

    @BeforeEach
    fun setup() {
        MockServer.setup()
        registry = BlockRegistry(MockServer.plugin)
        persistence = FluidBlockPersistence(MockServer.plugin)
        Blocks.initFluidFactory()
    }

    @AfterEach
    fun teardown() {
        MockServer.teardown()
    }

    @Test
    fun `save and load round-trip`() {
        val pump = FluidPump(MockServer.createLocation(1.0, 64.0, 2.0))
        pump.storeFluid(FluidType.WATER)
        registry.track(pump, "atlas:fluid_pump")

        persistence.save(registry)

        val loadRegistry = BlockRegistry(MockServer.plugin)
        persistence.load(loadRegistry)

        val loaded = loadRegistry.getAllBlocksWithIds()
        assertEquals(1, loaded.size)
        assertEquals("atlas:fluid_pump", loaded[0].second)
        assertEquals(FluidType.WATER, assertInstanceOf(FluidPump::class.java, loaded[0].first).storedFluid)
    }

    @Test
    fun `load from missing file does not error`() {
        val loadRegistry = BlockRegistry(MockServer.plugin)
        assertDoesNotThrow { persistence.load(loadRegistry) }
        assertEquals(0, loadRegistry.getAllBlocksWithIds().size)
    }

    @Test
    fun `fluid type LAVA persists correctly`() {
        val pump = FluidPump(MockServer.createLocation())
        pump.storeFluid(FluidType.LAVA)
        registry.track(pump, "atlas:fluid_pump")

        persistence.save(registry)

        val loadRegistry = BlockRegistry(MockServer.plugin)
        persistence.load(loadRegistry)

        val loadedLava = assertInstanceOf(FluidPump::class.java, loadRegistry.getAllBlocksWithIds().first().first)
        assertEquals(FluidType.LAVA, loadedLava.storedFluid)
    }

    @Test
    fun `fluid type NONE persists correctly`() {
        val pump = FluidPump(MockServer.createLocation())
        // storedFluid defaults to NONE
        registry.track(pump, "atlas:fluid_pump")

        persistence.save(registry)

        val loadRegistry = BlockRegistry(MockServer.plugin)
        persistence.load(loadRegistry)

        val loadedNone = assertInstanceOf(FluidPump::class.java, loadRegistry.getAllBlocksWithIds().first().first)
        assertEquals(FluidType.NONE, loadedNone.storedFluid)
    }

    @Test
    fun `facing direction persists for pipes`() {
        val pipe = FluidPipe(MockServer.createLocation())
        registry.track(pipe, "atlas:fluid_pipe")

        persistence.save(registry)

        val loadRegistry = BlockRegistry(MockServer.plugin)
        persistence.load(loadRegistry)

        val loaded = loadRegistry.getAllBlocksWithIds().first().first
        assertTrue(loaded is FluidPipe)
    }

    @Test
    fun `multiple fluid blocks save and load correctly`() {
        val pump = FluidPump(MockServer.createLocation(0.0, 64.0, 0.0))
        val pipe = FluidPipe(MockServer.createLocation(1.0, 64.0, 0.0))
        registry.track(pump, "atlas:fluid_pump")
        registry.track(pipe, "atlas:fluid_pipe")

        pump.storeFluid(FluidType.WATER)
        pipe.storeFluid(FluidType.LAVA)

        persistence.save(registry)

        val loadRegistry = BlockRegistry(MockServer.plugin)
        persistence.load(loadRegistry)

        val loaded = loadRegistry.getAllBlocksWithIds()
        assertEquals(2, loaded.size)
    }

    /** The mirror of the power case: the shared index must not spill a battery into this file. */
    @Test
    fun `fluid_blocks yml holds only fluid blocks when the registry also holds a battery`() {
        registry.track(FluidPipe(MockServer.createLocation()), FluidPipe.BLOCK_ID)
        registry.track(SmallBattery(MockServer.createLocation(x = 1.0)), SmallBattery.BLOCK_ID)

        persistence.save(registry)

        val saved = File(MockServer.dataFolder, "fluid_blocks.yml").readText()
        assertTrue("atlas:fluid_pipe" in saved, saved)
        assertFalse("atlas:small_battery" in saved, saved)
    }
}

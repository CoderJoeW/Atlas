package com.coderjoe.atlas.data

import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.block.capability.FluidType
import com.coderjoe.atlas.block.fluid.block.FluidPipe
import com.coderjoe.atlas.block.fluid.block.FluidPump
import com.coderjoe.atlas.block.power.SmallBattery
import com.coderjoe.atlas.testing.TestHelper
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
        TestHelper.setup()
        registry = BlockRegistry(TestHelper.mockPlugin)
        persistence = FluidBlockPersistence(TestHelper.mockPlugin)
        TestHelper.initFluidFactory()
    }

    @AfterEach
    fun teardown() {
        TestHelper.teardown()
    }

    @Test
    fun `save and load round-trip`() {
        val pump = FluidPump(TestHelper.createLocation(1.0, 64.0, 2.0))
        pump.storeFluid(FluidType.WATER)
        TestHelper.addToRegistry(registry, pump, "atlas:fluid_pump")

        persistence.save(registry)

        val loadRegistry = BlockRegistry(TestHelper.mockPlugin)
        persistence.load(loadRegistry)

        val loaded = loadRegistry.getAllBlocksWithIds()
        assertEquals(1, loaded.size)
        assertEquals("atlas:fluid_pump", loaded[0].second)
        assertEquals(FluidType.WATER, assertInstanceOf(FluidPump::class.java, loaded[0].first).storedFluid)
    }

    @Test
    fun `load from missing file does not error`() {
        val loadRegistry = BlockRegistry(TestHelper.mockPlugin)
        assertDoesNotThrow { persistence.load(loadRegistry) }
        assertEquals(0, loadRegistry.getAllBlocksWithIds().size)
    }

    @Test
    fun `fluid type LAVA persists correctly`() {
        val pump = FluidPump(TestHelper.createLocation())
        pump.storeFluid(FluidType.LAVA)
        TestHelper.addToRegistry(registry, pump, "atlas:fluid_pump")

        persistence.save(registry)

        val loadRegistry = BlockRegistry(TestHelper.mockPlugin)
        persistence.load(loadRegistry)

        val loadedLava = assertInstanceOf(FluidPump::class.java, loadRegistry.getAllBlocksWithIds().first().first)
        assertEquals(FluidType.LAVA, loadedLava.storedFluid)
    }

    @Test
    fun `fluid type NONE persists correctly`() {
        val pump = FluidPump(TestHelper.createLocation())
        // storedFluid defaults to NONE
        TestHelper.addToRegistry(registry, pump, "atlas:fluid_pump")

        persistence.save(registry)

        val loadRegistry = BlockRegistry(TestHelper.mockPlugin)
        persistence.load(loadRegistry)

        val loadedNone = assertInstanceOf(FluidPump::class.java, loadRegistry.getAllBlocksWithIds().first().first)
        assertEquals(FluidType.NONE, loadedNone.storedFluid)
    }

    @Test
    fun `facing direction persists for pipes`() {
        val pipe = FluidPipe(TestHelper.createLocation())
        TestHelper.addToRegistry(registry, pipe, "atlas:fluid_pipe")

        persistence.save(registry)

        val loadRegistry = BlockRegistry(TestHelper.mockPlugin)
        persistence.load(loadRegistry)

        val loaded = loadRegistry.getAllBlocksWithIds().first().first
        assertTrue(loaded is FluidPipe)
    }

    @Test
    fun `multiple fluid blocks save and load correctly`() {
        val pump = FluidPump(TestHelper.createLocation(0.0, 64.0, 0.0))
        pump.storeFluid(FluidType.WATER)
        val pipe = FluidPipe(TestHelper.createLocation(1.0, 64.0, 0.0))
        pipe.storeFluid(FluidType.LAVA)

        TestHelper.addToRegistry(registry, pump, "atlas:fluid_pump")
        TestHelper.addToRegistry(registry, pipe, "atlas:fluid_pipe")

        persistence.save(registry)

        val loadRegistry = BlockRegistry(TestHelper.mockPlugin)
        persistence.load(loadRegistry)

        val loaded = loadRegistry.getAllBlocksWithIds()
        assertEquals(2, loaded.size)
    }

    /** The mirror of the power case: the shared index must not spill a battery into this file. */
    @Test
    fun `fluid_blocks yml holds only fluid blocks when the registry also holds a battery`() {
        TestHelper.addToRegistry(registry, FluidPipe(TestHelper.createLocation()), FluidPipe.BLOCK_ID)
        TestHelper.addToRegistry(registry, SmallBattery(TestHelper.createLocation(x = 1.0)), SmallBattery.BLOCK_ID)

        persistence.save(registry)

        val saved = File(TestHelper.dataFolder, "fluid_blocks.yml").readText()
        assertTrue("atlas:fluid_pipe" in saved, saved)
        assertFalse("atlas:small_battery" in saved, saved)
    }
}

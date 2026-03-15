package com.coderjoe.atlas.fluid

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.fluid.block.FluidPipe
import com.coderjoe.atlas.fluid.block.FluidPump
import org.bukkit.block.BlockFace
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FluidBlockPersistenceTest {
    private lateinit var registry: FluidBlockRegistry
    private lateinit var persistence: FluidBlockPersistence

    @BeforeEach
    fun setup() {
        TestHelper.setup()
        registry = FluidBlockRegistry(TestHelper.mockPlugin)
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

        val loadRegistry = FluidBlockRegistry(TestHelper.mockPlugin)
        persistence.load(loadRegistry)

        val loaded = loadRegistry.getAllFluidBlocksWithIds()
        assertEquals(1, loaded.size)
        assertEquals("atlas:fluid_pump", loaded[0].second)
        assertEquals(FluidType.WATER, loaded[0].first.storedFluid)
    }

    @Test
    fun `load from missing file does not error`() {
        val loadRegistry = FluidBlockRegistry(TestHelper.mockPlugin)
        assertDoesNotThrow { persistence.load(loadRegistry) }
        assertEquals(0, loadRegistry.getAllFluidBlocksWithIds().size)
    }

    @Test
    fun `fluid type LAVA persists correctly`() {
        val pump = FluidPump(TestHelper.createLocation())
        pump.storeFluid(FluidType.LAVA)
        TestHelper.addToRegistry(registry, pump, "atlas:fluid_pump")

        persistence.save(registry)

        val loadRegistry = FluidBlockRegistry(TestHelper.mockPlugin)
        persistence.load(loadRegistry)

        assertEquals(FluidType.LAVA, loadRegistry.getAllFluidBlocksWithIds().first().first.storedFluid)
    }

    @Test
    fun `fluid type NONE persists correctly`() {
        val pump = FluidPump(TestHelper.createLocation())
        // storedFluid defaults to NONE
        TestHelper.addToRegistry(registry, pump, "atlas:fluid_pump")

        persistence.save(registry)

        val loadRegistry = FluidBlockRegistry(TestHelper.mockPlugin)
        persistence.load(loadRegistry)

        assertEquals(FluidType.NONE, loadRegistry.getAllFluidBlocksWithIds().first().first.storedFluid)
    }

    @Test
    fun `facing direction persists for pipes`() {
        val pipe = FluidPipe(TestHelper.createLocation(), BlockFace.EAST)
        TestHelper.addToRegistry(registry, pipe, "atlas:fluid_pipe")

        persistence.save(registry)

        val loadRegistry = FluidBlockRegistry(TestHelper.mockPlugin)
        persistence.load(loadRegistry)

        val loaded = loadRegistry.getAllFluidBlocksWithIds().first().first
        assertTrue(loaded is FluidPipe)
    }

    @Test
    fun `multiple fluid blocks save and load correctly`() {
        val pump = FluidPump(TestHelper.createLocation(0.0, 64.0, 0.0))
        pump.storeFluid(FluidType.WATER)
        val pipe = FluidPipe(TestHelper.createLocation(1.0, 64.0, 0.0), BlockFace.NORTH)
        pipe.storeFluid(FluidType.LAVA)

        TestHelper.addToRegistry(registry, pump, "atlas:fluid_pump")
        TestHelper.addToRegistry(registry, pipe, "atlas:fluid_pipe")

        persistence.save(registry)

        val loadRegistry = FluidBlockRegistry(TestHelper.mockPlugin)
        persistence.load(loadRegistry)

        val loaded = loadRegistry.getAllFluidBlocksWithIds()
        assertEquals(2, loaded.size)
    }
}

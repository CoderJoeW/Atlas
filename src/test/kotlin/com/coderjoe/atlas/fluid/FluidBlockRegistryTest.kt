package com.coderjoe.atlas.fluid

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.fluid.block.FluidPipe
import com.coderjoe.atlas.fluid.block.FluidPump
import org.bukkit.block.BlockFace
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

class FluidBlockRegistryTest {

    private lateinit var registry: FluidBlockRegistry

    @BeforeEach
    fun setup() {
        TestHelper.setup()
        registry = FluidBlockRegistry(TestHelper.mockPlugin)
    }

    @AfterEach
    fun teardown() {
        TestHelper.teardown()
    }

    @Test
    fun `locationKey produces correct format`() {
        val loc = TestHelper.createLocation(5.0, 100.0, -3.0)
        assertEquals("world:5,100,-3", FluidBlockRegistry.locationKey(loc))
    }

    @Test
    fun `register and get returns block`() {
        val loc = TestHelper.createLocation()
        val pump = FluidPump(loc)
        TestHelper.addToRegistry(registry, pump, "fluid_pump")

        assertSame(pump, registry.getFluidBlock(loc))
    }

    @Test
    fun `unregister removes and returns block`() {
        val loc = TestHelper.createLocation()
        val pump = FluidPump(loc)
        TestHelper.addToRegistry(registry, pump, "fluid_pump")

        val removed = registry.unregisterFluidBlock(loc)
        assertSame(pump, removed)
        assertNull(registry.getFluidBlock(loc))
    }

    @Test
    fun `unregister non-existent returns null`() {
        assertNull(registry.unregisterFluidBlock(TestHelper.createLocation(99.0, 99.0, 99.0)))
    }

    @Test
    fun `getAdjacentFluidBlock returns block in correct direction`() {
        val neighborLoc = TestHelper.createLocation(0.0, 64.0, -1.0) // NORTH
        val pump = FluidPump(neighborLoc)
        TestHelper.addToRegistry(registry, pump, "fluid_pump")

        val adjacent = registry.getAdjacentFluidBlock(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.NORTH)
        assertSame(pump, adjacent)
    }

    @Test
    fun `getAllFluidBlocksWithIds returns correct pairs`() {
        val pump = FluidPump(TestHelper.createLocation(0.0, 64.0, 0.0))
        val pipe = FluidPipe(TestHelper.createLocation(1.0, 64.0, 0.0), BlockFace.NORTH)

        TestHelper.addToRegistry(registry, pump, "fluid_pump")
        TestHelper.addToRegistry(registry, pipe, "fluid_pipe_north")

        val pairs = registry.getAllFluidBlocksWithIds()
        assertEquals(2, pairs.size)
        assertTrue(pairs.any { it.first === pump && it.second == "fluid_pump" })
        assertTrue(pairs.any { it.first === pipe && it.second == "fluid_pipe_north" })
    }

    @Test
    fun `stopAll clears registry`() {
        TestHelper.addToRegistry(registry, FluidPump(TestHelper.createLocation()), "fluid_pump")
        registry.stopAll()
        assertEquals(0, registry.getAllFluidBlocksWithIds().size)
    }

    @Test
    fun `instance is set on creation`() {
        assertSame(registry, FluidBlockRegistry.instance)
    }

    @Test
    fun `getAdjacentFluidBlock returns null when no block in direction`() {
        val loc = TestHelper.createLocation(0.0, 64.0, 0.0)
        assertNull(registry.getAdjacentFluidBlock(loc, BlockFace.NORTH))
    }

    @Test
    fun `registering at same location overwrites`() {
        val loc = TestHelper.createLocation()
        val pump1 = FluidPump(loc)
        val pump2 = FluidPump(loc)
        TestHelper.addToRegistry(registry, pump1, "fluid_pump")
        TestHelper.addToRegistry(registry, pump2, "fluid_pump")

        assertSame(pump2, registry.getFluidBlock(loc))
    }
}

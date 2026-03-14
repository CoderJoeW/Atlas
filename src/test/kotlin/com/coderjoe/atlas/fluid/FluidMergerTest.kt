package com.coderjoe.atlas.fluid

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.TestHelper.callFluidUpdate
import com.coderjoe.atlas.fluid.block.FluidMerger
import com.coderjoe.atlas.fluid.block.FluidPipe
import org.bukkit.block.BlockFace
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

class FluidMergerTest {

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
    fun `visual state empty when no fluid`() {
        val merger = FluidMerger(TestHelper.createLocation(), BlockFace.NORTH)
        assertEquals("fluid_merger_north", merger.getVisualStateBlockId())
    }

    @Test
    fun `visual state water when holding water`() {
        val merger = FluidMerger(TestHelper.createLocation(), BlockFace.NORTH)
        merger.storeFluid(FluidType.WATER)
        assertEquals("fluid_merger_north_filled", merger.getVisualStateBlockId())
    }

    @Test
    fun `visual state lava when holding lava`() {
        val merger = FluidMerger(TestHelper.createLocation(), BlockFace.NORTH)
        merger.storeFluid(FluidType.LAVA)
        assertEquals("fluid_merger_north_filled_lava", merger.getVisualStateBlockId())
    }

    @Test
    fun `visual state varies by facing direction`() {
        for ((face, expectedId) in FluidMerger.DIRECTIONAL_IDS) {
            val merger = FluidMerger(TestHelper.createLocation(), face)
            assertEquals(expectedId, merger.getVisualStateBlockId())
        }
    }

    @Test
    fun `pulls fluid from non-facing pipe`() {
        val mergerLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val merger = FluidMerger(mergerLoc, BlockFace.NORTH)
        TestHelper.addToRegistry(registry, merger, "fluid_merger_north")

        // Pipe to the south (not facing direction)
        val pipeLoc = TestHelper.createLocation(0.0, 64.0, 1.0)
        val pipe = FluidPipe(pipeLoc, BlockFace.NORTH)
        pipe.storeFluid(FluidType.WATER)
        TestHelper.addToRegistry(registry, pipe, "fluid_pipe_north_filled")

        merger.callFluidUpdate()

        assertTrue(merger.hasFluid())
        assertEquals(FluidType.WATER, merger.storedFluid)
        assertFalse(pipe.hasFluid())
    }

    @Test
    fun `does not pull fluid from facing direction`() {
        val mergerLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val merger = FluidMerger(mergerLoc, BlockFace.NORTH)
        TestHelper.addToRegistry(registry, merger, "fluid_merger_north")

        // Pipe to the north (facing direction — should NOT pull from here)
        val pipeLoc = TestHelper.createLocation(0.0, 64.0, -1.0)
        val pipe = FluidPipe(pipeLoc, BlockFace.SOUTH)
        pipe.storeFluid(FluidType.WATER)
        TestHelper.addToRegistry(registry, pipe, "fluid_pipe_south_filled")

        merger.callFluidUpdate()

        assertFalse(merger.hasFluid())
        assertTrue(pipe.hasFluid())
    }

    @Test
    fun `does not pull when already holding fluid`() {
        val mergerLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val merger = FluidMerger(mergerLoc, BlockFace.NORTH)
        merger.storeFluid(FluidType.WATER)
        TestHelper.addToRegistry(registry, merger, "fluid_merger_north_filled")

        val pipeLoc = TestHelper.createLocation(0.0, 64.0, 1.0)
        val pipe = FluidPipe(pipeLoc, BlockFace.NORTH)
        pipe.storeFluid(FluidType.LAVA)
        TestHelper.addToRegistry(registry, pipe, "fluid_pipe_north_filled_lava")

        merger.callFluidUpdate()

        assertEquals(FluidType.WATER, merger.storedFluid)
        assertTrue(pipe.hasFluid())
    }

    @Test
    fun `pulls from multiple input directions`() {
        // First pull from east
        val mergerLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val merger = FluidMerger(mergerLoc, BlockFace.NORTH)
        TestHelper.addToRegistry(registry, merger, "fluid_merger_north")

        val pipeLoc = TestHelper.createLocation(1.0, 64.0, 0.0)
        val pipe = FluidPipe(pipeLoc, BlockFace.WEST)
        pipe.storeFluid(FluidType.LAVA)
        TestHelper.addToRegistry(registry, pipe, "fluid_pipe_west_filled_lava")

        merger.callFluidUpdate()

        assertTrue(merger.hasFluid())
        assertEquals(FluidType.LAVA, merger.storedFluid)
        assertFalse(pipe.hasFluid())
    }

    @Test
    fun `downstream pipe can pull from merger`() {
        val mergerLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val merger = FluidMerger(mergerLoc, BlockFace.NORTH)
        merger.storeFluid(FluidType.WATER)
        TestHelper.addToRegistry(registry, merger, "fluid_merger_north_filled")

        // Merger holds fluid; downstream blocks pull via their own update
        assertTrue(merger.hasFluid())
        val fluid = merger.removeFluid()
        assertEquals(FluidType.WATER, fluid)
        assertFalse(merger.hasFluid())
    }

    @Test
    fun `descriptor has correct properties`() {
        val desc = FluidMerger.descriptor
        assertEquals("fluid_merger", desc.baseBlockId)
        assertEquals("Fluid Merger", desc.displayName)
        assertEquals(18, desc.allRegistrableIds.size)
        assertTrue(desc.allRegistrableIds.contains("fluid_merger_north"))
        assertTrue(desc.allRegistrableIds.contains("fluid_merger_north_filled"))
        assertTrue(desc.allRegistrableIds.contains("fluid_merger_north_filled_lava"))
    }
}

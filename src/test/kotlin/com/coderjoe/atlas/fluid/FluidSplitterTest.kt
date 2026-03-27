package com.coderjoe.atlas.fluid

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.TestHelper.callFluidUpdate
import com.coderjoe.atlas.fluid.block.FluidPipe
import com.coderjoe.atlas.fluid.block.FluidSplitter
import org.bukkit.block.BlockFace
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FluidSplitterTest {
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
    fun `visual state always returns BLOCK_ID`() {
        val splitter = FluidSplitter(TestHelper.createLocation(), BlockFace.NORTH)
        assertEquals("atlas:fluid_splitter", splitter.getVisualStateBlockId())
    }

    @Test
    fun `visual state returns BLOCK_ID when holding water`() {
        val splitter = FluidSplitter(TestHelper.createLocation(), BlockFace.NORTH)
        splitter.storeFluid(FluidType.WATER)
        assertEquals("atlas:fluid_splitter", splitter.getVisualStateBlockId())
    }

    @Test
    fun `visual state returns BLOCK_ID when holding lava`() {
        val splitter = FluidSplitter(TestHelper.createLocation(), BlockFace.NORTH)
        splitter.storeFluid(FluidType.LAVA)
        assertEquals("atlas:fluid_splitter", splitter.getVisualStateBlockId())
    }

    @Test
    fun `pulls fluid from opposite of facing direction`() {
        val splitterLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val splitter = FluidSplitter(splitterLoc, BlockFace.NORTH)
        TestHelper.addToRegistry(registry, splitter, "atlas:fluid_splitter")

        // Pipe to the south (opposite of facing = input)
        val pipeLoc = TestHelper.createLocation(0.0, 64.0, 1.0)
        val pipe = FluidPipe(pipeLoc, BlockFace.NORTH)
        pipe.storeFluid(FluidType.WATER)
        TestHelper.addToRegistry(registry, pipe, "atlas:fluid_pipe")

        splitter.callFluidUpdate()

        assertTrue(splitter.hasFluid())
        assertEquals(FluidType.WATER, splitter.storedFluid)
        assertFalse(pipe.hasFluid())
    }

    @Test
    fun `does not pull fluid from facing direction`() {
        val splitterLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val splitter = FluidSplitter(splitterLoc, BlockFace.NORTH)
        TestHelper.addToRegistry(registry, splitter, "atlas:fluid_splitter")

        // Pipe to the north (facing direction, not input)
        val pipeLoc = TestHelper.createLocation(0.0, 64.0, -1.0)
        val pipe = FluidPipe(pipeLoc, BlockFace.SOUTH)
        pipe.storeFluid(FluidType.WATER)
        TestHelper.addToRegistry(registry, pipe, "atlas:fluid_pipe")

        splitter.callFluidUpdate()

        assertFalse(splitter.hasFluid())
        assertTrue(pipe.hasFluid())
    }

    @Test
    fun `pushes fluid to output face neighbor`() {
        val splitterLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val splitter = FluidSplitter(splitterLoc, BlockFace.NORTH)
        splitter.storeFluid(FluidType.WATER)
        TestHelper.addToRegistry(registry, splitter, "atlas:fluid_splitter")

        // Pipe to the east (output face)
        val pipeLoc = TestHelper.createLocation(1.0, 64.0, 0.0)
        val pipe = FluidPipe(pipeLoc, BlockFace.EAST)
        TestHelper.addToRegistry(registry, pipe, "atlas:fluid_pipe")

        splitter.callFluidUpdate()

        assertFalse(splitter.hasFluid())
        assertTrue(pipe.hasFluid())
        assertEquals(FluidType.WATER, pipe.storedFluid)
    }

    @Test
    fun `does not push fluid to input face`() {
        val splitterLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val splitter = FluidSplitter(splitterLoc, BlockFace.NORTH)
        splitter.storeFluid(FluidType.WATER)
        TestHelper.addToRegistry(registry, splitter, "atlas:fluid_splitter")

        // Pipe to the south (input face = opposite of facing)
        val pipeLoc = TestHelper.createLocation(0.0, 64.0, 1.0)
        val pipe = FluidPipe(pipeLoc, BlockFace.NORTH)
        TestHelper.addToRegistry(registry, pipe, "atlas:fluid_pipe")

        splitter.callFluidUpdate()

        // Splitter should still have fluid since only output face neighbor is input
        assertTrue(splitter.hasFluid())
        assertFalse(pipe.hasFluid())
    }

    @Test
    fun `does not push to neighbor that already has fluid`() {
        val splitterLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val splitter = FluidSplitter(splitterLoc, BlockFace.NORTH)
        splitter.storeFluid(FluidType.WATER)
        TestHelper.addToRegistry(registry, splitter, "atlas:fluid_splitter")

        // Pipe to the east already has fluid
        val pipeLoc = TestHelper.createLocation(1.0, 64.0, 0.0)
        val pipe = FluidPipe(pipeLoc, BlockFace.EAST)
        pipe.storeFluid(FluidType.LAVA)
        TestHelper.addToRegistry(registry, pipe, "atlas:fluid_pipe")

        splitter.callFluidUpdate()

        assertTrue(splitter.hasFluid())
        assertEquals(FluidType.WATER, splitter.storedFluid)
        assertEquals(FluidType.LAVA, pipe.storedFluid)
    }

    @Test
    fun `pulls and pushes in same update`() {
        val splitterLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val splitter = FluidSplitter(splitterLoc, BlockFace.NORTH)
        TestHelper.addToRegistry(registry, splitter, "atlas:fluid_splitter")

        // Source pipe to the south (input face)
        val sourceLoc = TestHelper.createLocation(0.0, 64.0, 1.0)
        val source = FluidPipe(sourceLoc, BlockFace.NORTH)
        source.storeFluid(FluidType.LAVA)
        TestHelper.addToRegistry(registry, source, "atlas:fluid_pipe")

        // Target pipe to the east (output face)
        val targetLoc = TestHelper.createLocation(1.0, 64.0, 0.0)
        val target = FluidPipe(targetLoc, BlockFace.EAST)
        TestHelper.addToRegistry(registry, target, "atlas:fluid_pipe")

        splitter.callFluidUpdate()

        assertFalse(source.hasFluid())
        assertFalse(splitter.hasFluid())
        assertTrue(target.hasFluid())
        assertEquals(FluidType.LAVA, target.storedFluid)
    }

    @Test
    fun `descriptor has correct properties`() {
        val desc = FluidSplitter.descriptor
        assertEquals("atlas:fluid_splitter", desc.baseBlockId)
        assertEquals("Fluid Splitter", desc.displayName)
    }

    @Test
    fun `distributes fluid round-robin across multiple updates`() {
        // Splitter facing NORTH, input from SOUTH
        val splitterLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val splitter = FluidSplitter(splitterLoc, BlockFace.NORTH)
        TestHelper.addToRegistry(registry, splitter, "atlas:fluid_splitter")

        // Two output pipes: east and west (both empty)
        val eastLoc = TestHelper.createLocation(1.0, 64.0, 0.0)
        val eastPipe = FluidPipe(eastLoc, BlockFace.EAST)
        TestHelper.addToRegistry(registry, eastPipe, "atlas:fluid_pipe")

        val westLoc = TestHelper.createLocation(-1.0, 64.0, 0.0)
        val westPipe = FluidPipe(westLoc, BlockFace.WEST)
        TestHelper.addToRegistry(registry, westPipe, "atlas:fluid_pipe")

        // First update: give splitter fluid, push to one output
        splitter.storeFluid(FluidType.WATER)
        splitter.callFluidUpdate()

        val firstEast = eastPipe.hasFluid()
        val firstWest = westPipe.hasFluid()
        assertTrue(firstEast || firstWest, "fluid should go somewhere")
        assertFalse(firstEast && firstWest, "only one should receive")

        // Drain the pipes and do it again
        eastPipe.removeFluid()
        westPipe.removeFluid()
        splitter.storeFluid(FluidType.WATER)
        splitter.callFluidUpdate()

        val secondEast = eastPipe.hasFluid()
        val secondWest = westPipe.hasFluid()
        assertTrue(secondEast || secondWest, "fluid should go somewhere")

        // The two rounds should have gone to different targets
        assertTrue(
            firstEast != secondEast,
            "round-robin should alternate: firstEast=$firstEast, secondEast=$secondEast",
        )
    }
}

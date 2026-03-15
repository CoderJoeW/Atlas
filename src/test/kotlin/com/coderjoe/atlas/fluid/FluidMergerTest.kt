package com.coderjoe.atlas.fluid

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.TestHelper.callFluidUpdate
import com.coderjoe.atlas.fluid.block.FluidMerger
import com.coderjoe.atlas.fluid.block.FluidPipe
import org.bukkit.block.BlockFace
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
    fun `visual state always returns BLOCK_ID`() {
        val merger =
            FluidMerger(TestHelper.createLocation(), BlockFace.NORTH)
        assertEquals(
            "atlas:fluid_merger",
            merger.getVisualStateBlockId(),
        )
    }

    @Test
    fun `visual state returns BLOCK_ID when holding water`() {
        val merger =
            FluidMerger(TestHelper.createLocation(), BlockFace.NORTH)
        merger.storeFluid(FluidType.WATER)
        assertEquals(
            "atlas:fluid_merger",
            merger.getVisualStateBlockId(),
        )
    }

    @Test
    fun `visual state returns BLOCK_ID when holding lava`() {
        val merger =
            FluidMerger(TestHelper.createLocation(), BlockFace.NORTH)
        merger.storeFluid(FluidType.LAVA)
        assertEquals(
            "atlas:fluid_merger",
            merger.getVisualStateBlockId(),
        )
    }

    @Test
    fun `pulls fluid from non-facing pipe`() {
        val mergerLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val merger = FluidMerger(mergerLoc, BlockFace.NORTH)
        TestHelper.addToRegistry(
            registry,
            merger,
            "atlas:fluid_merger",
        )

        // Pipe to the south (not facing direction)
        val pipeLoc = TestHelper.createLocation(0.0, 64.0, 1.0)
        val pipe = FluidPipe(pipeLoc, BlockFace.NORTH)
        pipe.storeFluid(FluidType.WATER)
        TestHelper.addToRegistry(
            registry,
            pipe,
            "atlas:fluid_pipe",
        )

        merger.callFluidUpdate()

        assertTrue(merger.hasFluid())
        assertEquals(FluidType.WATER, merger.storedFluid)
        assertFalse(pipe.hasFluid())
    }

    @Test
    fun `does not pull fluid from facing direction`() {
        val mergerLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val merger = FluidMerger(mergerLoc, BlockFace.NORTH)
        TestHelper.addToRegistry(
            registry,
            merger,
            "atlas:fluid_merger",
        )

        // Pipe to the north (facing direction)
        val pipeLoc = TestHelper.createLocation(0.0, 64.0, -1.0)
        val pipe = FluidPipe(pipeLoc, BlockFace.SOUTH)
        pipe.storeFluid(FluidType.WATER)
        TestHelper.addToRegistry(
            registry,
            pipe,
            "atlas:fluid_pipe",
        )

        merger.callFluidUpdate()

        assertFalse(merger.hasFluid())
        assertTrue(pipe.hasFluid())
    }

    @Test
    fun `does not pull when already holding fluid`() {
        val mergerLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val merger = FluidMerger(mergerLoc, BlockFace.NORTH)
        merger.storeFluid(FluidType.WATER)
        TestHelper.addToRegistry(
            registry,
            merger,
            "atlas:fluid_merger",
        )

        val pipeLoc = TestHelper.createLocation(0.0, 64.0, 1.0)
        val pipe = FluidPipe(pipeLoc, BlockFace.NORTH)
        pipe.storeFluid(FluidType.LAVA)
        TestHelper.addToRegistry(
            registry,
            pipe,
            "atlas:fluid_pipe",
        )

        merger.callFluidUpdate()

        assertEquals(FluidType.WATER, merger.storedFluid)
        assertTrue(pipe.hasFluid())
    }

    @Test
    fun `pulls from multiple input directions`() {
        val mergerLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val merger = FluidMerger(mergerLoc, BlockFace.NORTH)
        TestHelper.addToRegistry(
            registry,
            merger,
            "atlas:fluid_merger",
        )

        val pipeLoc = TestHelper.createLocation(1.0, 64.0, 0.0)
        val pipe = FluidPipe(pipeLoc, BlockFace.WEST)
        pipe.storeFluid(FluidType.LAVA)
        TestHelper.addToRegistry(
            registry,
            pipe,
            "atlas:fluid_pipe",
        )

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
        TestHelper.addToRegistry(
            registry,
            merger,
            "atlas:fluid_merger",
        )

        assertTrue(merger.hasFluid())
        val fluid = merger.removeFluid()
        assertEquals(FluidType.WATER, fluid)
        assertFalse(merger.hasFluid())
    }

    @Test
    fun `descriptor has correct properties`() {
        val desc = FluidMerger.descriptor
        assertEquals("atlas:fluid_merger", desc.baseBlockId)
        assertEquals("Fluid Merger", desc.displayName)
    }
}

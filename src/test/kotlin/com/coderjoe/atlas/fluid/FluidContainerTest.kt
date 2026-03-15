package com.coderjoe.atlas.fluid

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.TestHelper.callFluidUpdate
import com.coderjoe.atlas.fluid.block.FluidContainer
import com.coderjoe.atlas.fluid.block.FluidPipe
import com.coderjoe.atlas.fluid.block.FluidPump
import org.bukkit.block.BlockFace
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame

class FluidContainerTest {

    @BeforeEach
    fun setup() {
        TestHelper.setup()
    }

    @AfterEach
    fun teardown() {
        TestHelper.teardown()
    }

    // --- Store/Remove multi-unit ---

    @Test
    fun `store fluid increments amount`() {
        val container = FluidContainer(TestHelper.createLocation(), BlockFace.NORTH)
        assertTrue(container.storeFluid(FluidType.WATER))
        assertEquals(1, container.storedAmount)
        assertEquals(FluidType.WATER, container.storedFluid)
    }

    @Test
    fun `store multiple units of same fluid`() {
        val container = FluidContainer(TestHelper.createLocation(), BlockFace.NORTH)
        for (i in 1..5) {
            assertTrue(container.storeFluid(FluidType.WATER))
        }
        assertEquals(5, container.storedAmount)
    }

    @Test
    fun `store up to max capacity`() {
        val container = FluidContainer(TestHelper.createLocation(), BlockFace.NORTH)
        for (i in 1..FluidContainer.MAX_CAPACITY) {
            assertTrue(container.storeFluid(FluidType.WATER))
        }
        assertEquals(FluidContainer.MAX_CAPACITY, container.storedAmount)
    }

    @Test
    fun `store rejects when full`() {
        val container = FluidContainer(TestHelper.createLocation(), BlockFace.NORTH)
        for (i in 1..FluidContainer.MAX_CAPACITY) {
            container.storeFluid(FluidType.WATER)
        }
        assertFalse(container.storeFluid(FluidType.WATER))
        assertEquals(FluidContainer.MAX_CAPACITY, container.storedAmount)
    }

    @Test
    fun `store rejects different fluid type`() {
        val container = FluidContainer(TestHelper.createLocation(), BlockFace.NORTH)
        container.storeFluid(FluidType.WATER)
        assertFalse(container.storeFluid(FluidType.LAVA))
        assertEquals(1, container.storedAmount)
        assertEquals(FluidType.WATER, container.storedFluid)
    }

    @Test
    fun `remove fluid decrements amount`() {
        val container = FluidContainer(TestHelper.createLocation(), BlockFace.NORTH)
        container.storeFluid(FluidType.WATER)
        container.storeFluid(FluidType.WATER)
        container.storeFluid(FluidType.WATER)

        val removed = container.removeFluid()
        assertEquals(FluidType.WATER, removed)
        assertEquals(2, container.storedAmount)
    }

    @Test
    fun `remove fluid clears type at zero`() {
        val container = FluidContainer(TestHelper.createLocation(), BlockFace.NORTH)
        container.storeFluid(FluidType.WATER)

        val removed = container.removeFluid()
        assertEquals(FluidType.WATER, removed)
        assertEquals(0, container.storedAmount)
        assertEquals(FluidType.NONE, container.storedFluid)
    }

    @Test
    fun `remove from empty returns NONE`() {
        val container = FluidContainer(TestHelper.createLocation(), BlockFace.NORTH)
        assertEquals(FluidType.NONE, container.removeFluid())
    }

    @Test
    fun `hasFluid returns true when amount greater than zero`() {
        val container = FluidContainer(TestHelper.createLocation(), BlockFace.NORTH)
        assertFalse(container.hasFluid())
        container.storeFluid(FluidType.WATER)
        assertTrue(container.hasFluid())
    }

    // --- canRemoveFluidFrom ---

    @Test
    fun `canRemoveFluidFrom returns true for front face`() {
        val container = FluidContainer(TestHelper.createLocation(), BlockFace.NORTH)
        container.storeFluid(FluidType.WATER)
        assertTrue(container.canRemoveFluidFrom(BlockFace.NORTH))
    }

    @Test
    fun `canRemoveFluidFrom returns false for non-front face`() {
        val container = FluidContainer(TestHelper.createLocation(), BlockFace.NORTH)
        container.storeFluid(FluidType.WATER)
        assertFalse(container.canRemoveFluidFrom(BlockFace.SOUTH))
        assertFalse(container.canRemoveFluidFrom(BlockFace.EAST))
    }

    @Test
    fun `canRemoveFluidFrom returns false when empty`() {
        val container = FluidContainer(TestHelper.createLocation(), BlockFace.NORTH)
        assertFalse(container.canRemoveFluidFrom(BlockFace.NORTH))
    }

    // --- Fill level ---

    @Test
    fun `fill level empty at 0`() {
        val container = FluidContainer(TestHelper.createLocation(), BlockFace.NORTH)
        assertEquals("empty", container.getFillLevel())
    }

    @Test
    fun `fill level low at 1 to 3`() {
        val container = FluidContainer(TestHelper.createLocation(), BlockFace.NORTH)
        for (i in 1..3) {
            container.storeFluid(FluidType.WATER)
            assertEquals("low", container.getFillLevel(), "Expected low at amount $i")
        }
    }

    @Test
    fun `fill level medium at 4 to 7`() {
        val container = FluidContainer(TestHelper.createLocation(), BlockFace.NORTH)
        repeat(4) { container.storeFluid(FluidType.WATER) }
        assertEquals("medium", container.getFillLevel())
        repeat(3) { container.storeFluid(FluidType.WATER) }
        assertEquals("medium", container.getFillLevel())
    }

    @Test
    fun `fill level full at 8 to 10`() {
        val container = FluidContainer(TestHelper.createLocation(), BlockFace.NORTH)
        repeat(8) { container.storeFluid(FluidType.WATER) }
        assertEquals("full", container.getFillLevel())
        repeat(2) { container.storeFluid(FluidType.WATER) }
        assertEquals("full", container.getFillLevel())
    }

    // --- Visual state ---

    @Test
    fun `visual state empty for all directions`() {
        for ((face, id) in FluidContainer.DIRECTIONAL_IDS) {
            val container = FluidContainer(TestHelper.createLocation(), face)
            assertEquals(id, container.getVisualStateBlockId())
        }
    }

    @Test
    fun `visual state water low`() {
        val container = FluidContainer(TestHelper.createLocation(), BlockFace.NORTH)
        container.storeFluid(FluidType.WATER)
        assertEquals("fluid_container_north_water_low", container.getVisualStateBlockId())
    }

    @Test
    fun `visual state water medium`() {
        val container = FluidContainer(TestHelper.createLocation(), BlockFace.SOUTH)
        repeat(5) { container.storeFluid(FluidType.WATER) }
        assertEquals("fluid_container_south_water_medium", container.getVisualStateBlockId())
    }

    @Test
    fun `visual state water full`() {
        val container = FluidContainer(TestHelper.createLocation(), BlockFace.EAST)
        repeat(10) { container.storeFluid(FluidType.WATER) }
        assertEquals("fluid_container_east_water_full", container.getVisualStateBlockId())
    }

    @Test
    fun `visual state lava low`() {
        val container = FluidContainer(TestHelper.createLocation(), BlockFace.WEST)
        container.storeFluid(FluidType.LAVA)
        assertEquals("fluid_container_west_lava_low", container.getVisualStateBlockId())
    }

    @Test
    fun `visual state returns empty after draining all fluid`() {
        val container = FluidContainer(TestHelper.createLocation(), BlockFace.UP)
        container.storeFluid(FluidType.WATER)
        container.removeFluid()
        assertEquals("fluid_container_up", container.getVisualStateBlockId())
    }

    // --- facingFromBlockId ---

    @Test
    fun `facingFromBlockId returns correct face for directional IDs`() {
        for ((face, id) in FluidContainer.DIRECTIONAL_IDS) {
            assertEquals(face, FluidContainer.facingFromBlockId(id))
        }
    }

    @Test
    fun `facingFromBlockId returns correct face for filled variant IDs`() {
        assertEquals(BlockFace.NORTH, FluidContainer.facingFromBlockId("fluid_container_north_water_low"))
        assertEquals(BlockFace.SOUTH, FluidContainer.facingFromBlockId("fluid_container_south_lava_full"))
    }

    @Test
    fun `facingFromBlockId returns null for unknown`() {
        assertNull(FluidContainer.facingFromBlockId("unknown_block"))
    }

    // --- ALL_VARIANT_IDS ---

    @Test
    fun `ALL_VARIANT_IDS contains 42 entries`() {
        assertEquals(42, FluidContainer.ALL_VARIANT_IDS.size)
    }

    @Test
    fun `ALL_VARIANT_IDS contains all directional IDs`() {
        for (id in FluidContainer.DIRECTIONAL_IDS.values) {
            assertTrue(id in FluidContainer.ALL_VARIANT_IDS, "Missing $id")
        }
    }

    // --- Directional pull ---

    @Test
    fun `container pulls from pipe behind it`() {
        val fluidRegistry = FluidBlockRegistry(TestHelper.mockPlugin)

        val container = FluidContainer(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.SOUTH)
        val pipe = FluidPipe(TestHelper.createLocation(0.0, 64.0, -1.0), BlockFace.SOUTH)
        pipe.storeFluid(FluidType.WATER)

        TestHelper.addToRegistry(fluidRegistry, container, "fluid_container_south")
        TestHelper.addToRegistry(fluidRegistry, pipe, "fluid_pipe_south")

        container.callFluidUpdate()
        assertEquals(FluidType.WATER, container.storedFluid)
        assertEquals(1, container.storedAmount)
        assertEquals(FluidType.NONE, pipe.storedFluid)
    }

    @Test
    fun `container pulls from pump behind it`() {
        val fluidRegistry = FluidBlockRegistry(TestHelper.mockPlugin)

        val container = FluidContainer(TestHelper.createLocation(0.0, 64.0, 1.0), BlockFace.SOUTH)
        val pump = FluidPump(TestHelper.createLocation(0.0, 64.0, 0.0))
        pump.storeFluid(FluidType.WATER)

        val cauldronField = FluidPump::class.java.getDeclaredField("cauldronFace")
        cauldronField.isAccessible = true
        cauldronField.set(pump, BlockFace.NORTH)

        TestHelper.addToRegistry(fluidRegistry, container, "fluid_container_south")
        TestHelper.addToRegistry(fluidRegistry, pump, "fluid_pump")

        container.callFluidUpdate()
        assertEquals(FluidType.WATER, container.storedFluid)
        assertEquals(1, container.storedAmount)
    }

    @Test
    fun `container pulls from another container behind it`() {
        val fluidRegistry = FluidBlockRegistry(TestHelper.mockPlugin)

        val container1 = FluidContainer(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.SOUTH)
        container1.storeFluid(FluidType.LAVA)

        val container2 = FluidContainer(TestHelper.createLocation(0.0, 64.0, 1.0), BlockFace.SOUTH)

        TestHelper.addToRegistry(fluidRegistry, container1, "fluid_container_south")
        TestHelper.addToRegistry(fluidRegistry, container2, "fluid_container_south")

        container2.callFluidUpdate()
        assertEquals(FluidType.LAVA, container2.storedFluid)
        assertEquals(1, container2.storedAmount)
        assertEquals(0, container1.storedAmount)
    }

    @Test
    fun `container does not pull when full`() {
        val fluidRegistry = FluidBlockRegistry(TestHelper.mockPlugin)

        val container = FluidContainer(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.SOUTH)
        repeat(FluidContainer.MAX_CAPACITY) { container.storeFluid(FluidType.WATER) }

        val pipe = FluidPipe(TestHelper.createLocation(0.0, 64.0, -1.0), BlockFace.SOUTH)
        pipe.storeFluid(FluidType.WATER)

        TestHelper.addToRegistry(fluidRegistry, container, "fluid_container_south")
        TestHelper.addToRegistry(fluidRegistry, pipe, "fluid_pipe_south")

        container.callFluidUpdate()
        assertTrue(pipe.hasFluid()) // pipe still has fluid
    }

    @Test
    fun `container rejects mismatched fluid from pipe`() {
        val fluidRegistry = FluidBlockRegistry(TestHelper.mockPlugin)

        val container = FluidContainer(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.SOUTH)
        container.storeFluid(FluidType.WATER)

        val pipe = FluidPipe(TestHelper.createLocation(0.0, 64.0, -1.0), BlockFace.SOUTH)
        pipe.storeFluid(FluidType.LAVA)

        TestHelper.addToRegistry(fluidRegistry, container, "fluid_container_south")
        TestHelper.addToRegistry(fluidRegistry, pipe, "fluid_pipe_south")

        container.callFluidUpdate()
        assertTrue(pipe.hasFluid()) // pipe keeps its lava since container rejected it
        assertEquals(1, container.storedAmount) // container unchanged
    }

    // --- Pipe pulling from container ---

    @Test
    fun `pipe pulls from container front face`() {
        val fluidRegistry = FluidBlockRegistry(TestHelper.mockPlugin)

        // Container facing SOUTH (front=SOUTH), pipe at z=1 facing SOUTH (pulls from NORTH=z-1=container)
        val container = FluidContainer(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.SOUTH)
        container.storeFluid(FluidType.WATER)

        val pipe = FluidPipe(TestHelper.createLocation(0.0, 64.0, 1.0), BlockFace.SOUTH)

        TestHelper.addToRegistry(fluidRegistry, container, "fluid_container_south")
        TestHelper.addToRegistry(fluidRegistry, pipe, "fluid_pipe_south")

        pipe.callFluidUpdate()
        assertEquals(FluidType.WATER, pipe.storedFluid)
        assertEquals(0, container.storedAmount)
    }

    @Test
    fun `pipe cannot pull from container non-front face`() {
        val fluidRegistry = FluidBlockRegistry(TestHelper.mockPlugin)

        // Container facing NORTH (front=NORTH), pipe behind at z=1 facing SOUTH pulls from NORTH=z-1
        // But container faces NORTH, so front is NORTH. Pipe pulling from SOUTH direction won't match.
        val container = FluidContainer(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.NORTH)
        container.storeFluid(FluidType.WATER)

        // Pipe at z=1, facing SOUTH, pulls from behind (NORTH side = z-1 = container)
        // It calls canRemoveFluidFrom(SOUTH) on container — container facing NORTH, so front is NORTH, not SOUTH
        val pipe = FluidPipe(TestHelper.createLocation(0.0, 64.0, 1.0), BlockFace.SOUTH)

        TestHelper.addToRegistry(fluidRegistry, container, "fluid_container_north")
        TestHelper.addToRegistry(fluidRegistry, pipe, "fluid_pipe_south")

        pipe.callFluidUpdate()
        assertEquals(FluidType.NONE, pipe.storedFluid) // could not pull
        assertEquals(1, container.storedAmount) // unchanged
    }

    // --- Persistence ---

    @Test
    fun `restoreState sets type and amount`() {
        val container = FluidContainer(TestHelper.createLocation(), BlockFace.NORTH)
        container.restoreState(FluidType.LAVA, 7)
        assertEquals(FluidType.LAVA, container.storedFluid)
        assertEquals(7, container.storedAmount)
    }

    @Test
    fun `restoreState clamps to max capacity`() {
        val container = FluidContainer(TestHelper.createLocation(), BlockFace.NORTH)
        container.restoreState(FluidType.WATER, 99)
        assertEquals(FluidContainer.MAX_CAPACITY, container.storedAmount)
    }

    @Test
    fun `FluidBlockData captures container facing and storedAmount`() {
        val container = FluidContainer(TestHelper.createLocation(), BlockFace.EAST)
        repeat(5) { container.storeFluid(FluidType.WATER) }

        val data = FluidBlockData.fromFluidBlock(container, "fluid_container_east")
        assertEquals("EAST", data.facing)
        assertEquals(5, data.storedAmount)
        assertEquals("WATER", data.fluidType)
    }
}

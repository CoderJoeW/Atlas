package com.coderjoe.atlas.block.fluid

import com.coderjoe.atlas.block.AtlasBlock
import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.block.capability.FluidType
import com.coderjoe.atlas.testing.MockServer
import org.bukkit.block.BlockFace
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FluidContainerTest {
    @BeforeEach
    fun setup() {
        MockServer.setup()
    }

    @AfterEach
    fun teardown() {
        MockServer.teardown()
    }

    // --- Store/Remove multi-unit ---

    @Test
    fun `store fluid increments amount`() {
        val container =
            FluidContainer(MockServer.createLocation())
        assertTrue(container.storeFluid(FluidType.WATER))
        assertEquals(1, container.storedAmount)
        assertEquals(FluidType.WATER, container.storedFluid)
    }

    @Test
    fun `store multiple units of same fluid`() {
        val container =
            FluidContainer(MockServer.createLocation())
        for (i in 1..5) {
            assertTrue(container.storeFluid(FluidType.WATER))
        }
        assertEquals(5, container.storedAmount)
    }

    @Test
    fun `store up to max capacity`() {
        val container =
            FluidContainer(MockServer.createLocation())
        for (i in 1..FluidContainer.MAX_CAPACITY) {
            assertTrue(container.storeFluid(FluidType.WATER))
        }
        assertEquals(FluidContainer.MAX_CAPACITY, container.storedAmount)
    }

    @Test
    fun `store rejects when full`() {
        val container =
            FluidContainer(MockServer.createLocation())
        for (i in 1..FluidContainer.MAX_CAPACITY) {
            container.storeFluid(FluidType.WATER)
        }
        assertFalse(container.storeFluid(FluidType.WATER))
        assertEquals(FluidContainer.MAX_CAPACITY, container.storedAmount)
    }

    @Test
    fun `store rejects different fluid type`() {
        val container =
            FluidContainer(MockServer.createLocation())
        container.storeFluid(FluidType.WATER)
        assertFalse(container.storeFluid(FluidType.LAVA))
        assertEquals(1, container.storedAmount)
        assertEquals(FluidType.WATER, container.storedFluid)
    }

    @Test
    fun `remove fluid decrements amount`() {
        val container =
            FluidContainer(MockServer.createLocation())
        container.storeFluid(FluidType.WATER)
        container.storeFluid(FluidType.WATER)
        container.storeFluid(FluidType.WATER)

        val removed = container.removeFluid()
        assertEquals(FluidType.WATER, removed)
        assertEquals(2, container.storedAmount)
    }

    @Test
    fun `remove fluid clears type at zero`() {
        val container =
            FluidContainer(MockServer.createLocation())
        container.storeFluid(FluidType.WATER)

        val removed = container.removeFluid()
        assertEquals(FluidType.WATER, removed)
        assertEquals(0, container.storedAmount)
        assertEquals(FluidType.NONE, container.storedFluid)
    }

    @Test
    fun `remove from empty returns NONE`() {
        val container =
            FluidContainer(MockServer.createLocation())
        assertEquals(FluidType.NONE, container.removeFluid())
    }

    @Test
    fun `hasFluid returns true when amount greater than zero`() {
        val container =
            FluidContainer(MockServer.createLocation())
        assertFalse(container.hasFluid())
        container.storeFluid(FluidType.WATER)
        assertTrue(container.hasFluid())
    }

    // --- a tank has no facing: it gives out and takes in through every side ---

    @Test
    fun `a loaded tank gives out through any side`() {
        val container = FluidContainer(MockServer.createLocation())
        container.storeFluid(FluidType.WATER)

        for (face in AtlasBlock.ADJACENT_FACES) {
            assertTrue(container.canProvideFluid(face), "should give out toward $face")
        }
    }

    @Test
    fun `an empty tank gives out nothing`() {
        val container = FluidContainer(MockServer.createLocation())

        for (face in AtlasBlock.ADJACENT_FACES) {
            assertFalse(container.canProvideFluid(face), "nothing to give toward $face")
        }
    }

    @Test
    fun `a tank takes fluid in through any side`() {
        val container = FluidContainer(MockServer.createLocation())

        for (face in AtlasBlock.ADJACENT_FACES) {
            assertTrue(container.canAcceptFluid(face, FluidType.WATER), "should fill from $face")
        }
    }

    @Test
    fun `a tank refuses a fluid that does not match what it already holds`() {
        val container = FluidContainer(MockServer.createLocation())
        container.storeFluid(FluidType.WATER)

        assertFalse(container.canAcceptFluid(BlockFace.NORTH, FluidType.LAVA))
        assertTrue(container.canAcceptFluid(BlockFace.NORTH, FluidType.WATER))
    }

    @Test
    fun `a full tank takes nothing more`() {
        val container = FluidContainer(MockServer.createLocation())
        repeat(FluidContainer.MAX_CAPACITY) { container.storeFluid(FluidType.WATER) }

        for (face in AtlasBlock.ADJACENT_FACES) {
            assertFalse(container.canAcceptFluid(face, FluidType.WATER), "full, so nothing from $face")
        }
    }

    // --- Fill level: five bars over a capacity of twenty ---

    @Test
    fun `fill level 0 at empty`() {
        val container =
            FluidContainer(MockServer.createLocation())
        assertEquals(0, container.getFillLevel())
    }

    @Test
    fun `each bar on the gauge is worth four units`() {
        val container = FluidContainer(MockServer.createLocation())

        // five bars over a capacity of twenty, so a bar lights every fourth unit and a tank
        // holding anything at all shows at least one
        val expected = listOf(1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5)
        for ((index, bars) in expected.withIndex()) {
            container.storeFluid(FluidType.WATER)
            assertEquals(bars, container.getFillLevel(), "at ${index + 1} units")
        }
    }

    @Test
    fun `a full tank shows every bar`() {
        val container = FluidContainer(MockServer.createLocation())
        repeat(FluidContainer.MAX_CAPACITY) { container.storeFluid(FluidType.WATER) }
        assertEquals(FluidContainer.FILL_LEVELS, container.getFillLevel())
    }

    // --- Visual state (now always returns BLOCK_ID) ---

    @Test
    fun `visual state always returns BLOCK_ID`() {
        val container =
            FluidContainer(MockServer.createLocation())
        assertEquals(
            "atlas:fluid_container",
            container.getVisualStateBlockId(),
        )
    }

    @Test
    fun `visual state returns BLOCK_ID with water`() {
        val container =
            FluidContainer(MockServer.createLocation())
        container.storeFluid(FluidType.WATER)
        assertEquals(
            "atlas:fluid_container",
            container.getVisualStateBlockId(),
        )
    }

    @Test
    fun `visual state returns BLOCK_ID after draining`() {
        val container =
            FluidContainer(MockServer.createLocation())
        container.storeFluid(FluidType.WATER)
        container.removeFluid()
        assertEquals(
            "atlas:fluid_container",
            container.getVisualStateBlockId(),
        )
    }

    // --- a tank is filled by others; it never reaches out for anything itself ---

    @Test
    fun `a pipe run delivers into the tank when the pump pushes`() {
        val fluidRegistry = BlockRegistry(MockServer.plugin)

        val container = FluidContainer(MockServer.createLocation(0.0, 64.0, 0.0))
        val pipe = FluidPipe(MockServer.createLocation(0.0, 64.0, -1.0))
        val pump = FluidPump(MockServer.createLocation(0.0, 64.0, -2.0))
        pump.storeFluid(FluidType.WATER)
        pump.cauldronFace = BlockFace.NORTH

        fluidRegistry.track(container, "atlas:fluid_container")
        fluidRegistry.track(pipe, "atlas:fluid_pipe")
        fluidRegistry.track(pump, "atlas:fluid_pump")

        // the pump hands its unit to the run, and the run finds the tank on the other end
        pump.fluidUpdate()

        assertEquals(FluidType.WATER, container.storedFluid)
        assertEquals(1, container.storedAmount)
        assertEquals(FluidType.NONE, pump.storedFluid)
    }

    @Test
    fun `a pump beside the tank fills it directly`() {
        val fluidRegistry = BlockRegistry(MockServer.plugin)

        val container = FluidContainer(MockServer.createLocation(0.0, 64.0, 1.0))
        val pump = FluidPump(MockServer.createLocation(0.0, 64.0, 0.0))
        pump.storeFluid(FluidType.WATER)

        fluidRegistry.track(container, "atlas:fluid_container")
        fluidRegistry.track(pump, "atlas:fluid_pump")

        pump.fluidUpdate()

        assertEquals(FluidType.WATER, container.storedFluid)
        assertEquals(1, container.storedAmount)
        assertEquals(FluidType.NONE, pump.storedFluid)
    }

    @Test
    fun `a tank never drains the tank next to it`() {
        val fluidRegistry = BlockRegistry(MockServer.plugin)

        val full = FluidContainer(MockServer.createLocation(0.0, 64.0, 0.0))
        full.storeFluid(FluidType.LAVA)
        val empty = FluidContainer(MockServer.createLocation(0.0, 64.0, 1.0))

        fluidRegistry.track(full, "atlas:fluid_container")
        fluidRegistry.track(empty, "atlas:fluid_container")

        empty.fluidUpdate()
        full.fluidUpdate()

        assertEquals(FluidType.LAVA, full.storedFluid, "the loaded tank keeps what it holds")
        assertEquals(FluidType.NONE, empty.storedFluid, "and the empty one stays empty")
    }

    @Test
    fun `container does not fill when full`() {
        val fluidRegistry = BlockRegistry(MockServer.plugin)

        val container = FluidContainer(MockServer.createLocation(0.0, 64.0, 0.0))
        container.restoreState(FluidType.WATER, FluidContainer.MAX_CAPACITY)

        val pump = FluidPump(MockServer.createLocation(0.0, 64.0, -1.0))
        pump.storeFluid(FluidType.WATER)
        pump.cauldronFace = BlockFace.NORTH

        fluidRegistry.track(container, "atlas:fluid_container")
        fluidRegistry.track(pump, "atlas:fluid_pump")

        container.fluidUpdate()

        assertTrue(pump.hasFluid(), "a full container leaves the source alone")
        assertEquals(FluidContainer.MAX_CAPACITY, container.storedAmount)
    }

    @Test
    fun `container rejects a fluid that does not match what it holds`() {
        val fluidRegistry = BlockRegistry(MockServer.plugin)

        val container = FluidContainer(MockServer.createLocation(0.0, 64.0, 0.0))
        container.restoreState(FluidType.WATER, 1)

        val pump = FluidPump(MockServer.createLocation(0.0, 64.0, -1.0))
        pump.storeFluid(FluidType.LAVA)
        pump.cauldronFace = BlockFace.NORTH

        fluidRegistry.track(container, "atlas:fluid_container")
        fluidRegistry.track(pump, "atlas:fluid_pump")

        container.fluidUpdate()

        assertTrue(pump.hasFluid(), "lava must not go into a water tank")
        assertEquals(1, container.storedAmount)
        assertEquals(FluidType.WATER, container.storedFluid)
    }

    // --- Pipe pulling from container ---

    @Test
    fun `a run offers what a container is giving through its front`() {
        val fluidRegistry = BlockRegistry(MockServer.plugin)

        val container = FluidContainer(MockServer.createLocation(0.0, 64.0, 0.0))
        container.restoreState(FluidType.WATER, 1)
        val pipe = FluidPipe(MockServer.createLocation(0.0, 64.0, 1.0))

        fluidRegistry.track(container, "atlas:fluid_container")
        fluidRegistry.track(pipe, "atlas:fluid_pipe")

        assertTrue(pipe.hasFluid())
        assertEquals(FluidType.WATER, pipe.removeFluid())
        assertEquals(0, container.storedAmount)
    }

    @Test
    fun `pipe cannot pull from container non-front face`() {
        val fluidRegistry = BlockRegistry(MockServer.plugin)

        val container =
            FluidContainer(MockServer.createLocation(0.0, 64.0, 0.0))
        container.storeFluid(FluidType.WATER)

        val pipe =
            FluidPipe(MockServer.createLocation(0.0, 64.0, 1.0))

        fluidRegistry.track(
            container,
            "atlas:fluid_container",
        )
        fluidRegistry.track(
            pipe,
            "atlas:fluid_pipe",
        )

        pipe.fluidUpdate()
        assertEquals(FluidType.NONE, pipe.storedFluid) // could not pull
        assertEquals(1, container.storedAmount) // unchanged
    }

    // --- Persistence ---

    @Test
    fun `restoreState sets type and amount`() {
        val container =
            FluidContainer(MockServer.createLocation())
        container.restoreState(FluidType.LAVA, 7)
        assertEquals(FluidType.LAVA, container.storedFluid)
        assertEquals(7, container.storedAmount)
    }

    @Test
    fun `restoreState clamps to max capacity`() {
        val container =
            FluidContainer(MockServer.createLocation())
        container.restoreState(FluidType.WATER, 99)
        assertEquals(FluidContainer.MAX_CAPACITY, container.storedAmount)
    }
}

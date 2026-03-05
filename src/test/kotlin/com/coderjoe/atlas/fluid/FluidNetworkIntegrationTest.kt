package com.coderjoe.atlas.fluid

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.TestHelper.callFluidUpdate
import com.coderjoe.atlas.fluid.block.FluidPipe
import com.coderjoe.atlas.fluid.block.FluidPump
import org.bukkit.block.BlockFace
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

class FluidNetworkIntegrationTest {

    private lateinit var fluidRegistry: FluidBlockRegistry

    @BeforeEach
    fun setup() {
        TestHelper.setup()
        fluidRegistry = FluidBlockRegistry(TestHelper.mockPlugin)
    }

    @AfterEach
    fun teardown() {
        TestHelper.teardown()
    }

    @Test
    fun `pump to pipe transfer`() {
        val pump = FluidPump(TestHelper.createLocation(0.0, 64.0, 0.0))
        pump.storeFluid(FluidType.WATER)

        // Set cauldronFace to NORTH so canRemoveFluidFrom(SOUTH) works
        val cauldronField = FluidPump::class.java.getDeclaredField("cauldronFace")
        cauldronField.isAccessible = true
        cauldronField.set(pump, BlockFace.NORTH)

        // Pipe at z=1, facing SOUTH (pulls from NORTH = z-1 = pump)
        val pipe = FluidPipe(TestHelper.createLocation(0.0, 64.0, 1.0), BlockFace.SOUTH)

        TestHelper.addToRegistry(fluidRegistry, pump, "fluid_pump")
        TestHelper.addToRegistry(fluidRegistry, pipe, "fluid_pipe_south")

        pipe.callFluidUpdate()
        assertEquals(FluidType.WATER, pipe.storedFluid)
        assertEquals(FluidType.NONE, pump.storedFluid)
    }

    @Test
    fun `pipe to pipe chain transfer`() {
        val pipe1 = FluidPipe(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.SOUTH)
        pipe1.storeFluid(FluidType.LAVA)

        val pipe2 = FluidPipe(TestHelper.createLocation(0.0, 64.0, 1.0), BlockFace.SOUTH)

        TestHelper.addToRegistry(fluidRegistry, pipe1, "fluid_pipe_south")
        TestHelper.addToRegistry(fluidRegistry, pipe2, "fluid_pipe_south")

        pipe2.callFluidUpdate()
        assertEquals(FluidType.LAVA, pipe2.storedFluid)
        assertEquals(FluidType.NONE, pipe1.storedFluid)
    }

    @Test
    fun `pipe only pulls from behind`() {
        // Pipe facing NORTH, should pull from SOUTH (z+1)
        val pipe = FluidPipe(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.NORTH)

        // Source pipe to the EAST (not behind)
        val source = FluidPipe(TestHelper.createLocation(1.0, 64.0, 0.0), BlockFace.NORTH)
        source.storeFluid(FluidType.WATER)

        TestHelper.addToRegistry(fluidRegistry, pipe, "fluid_pipe_north")
        TestHelper.addToRegistry(fluidRegistry, source, "fluid_pipe_north")

        pipe.callFluidUpdate()
        assertEquals(FluidType.NONE, pipe.storedFluid) // did not pull from side
    }

    @Test
    fun `pipe-to-pipe preserves fluid type`() {
        val pipe1 = FluidPipe(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.SOUTH)
        pipe1.storeFluid(FluidType.WATER)

        val pipe2 = FluidPipe(TestHelper.createLocation(0.0, 64.0, 1.0), BlockFace.SOUTH)

        TestHelper.addToRegistry(fluidRegistry, pipe1, "fluid_pipe_south")
        TestHelper.addToRegistry(fluidRegistry, pipe2, "fluid_pipe_south")

        pipe2.callFluidUpdate()
        assertEquals(FluidType.WATER, pipe2.storedFluid) // same type
    }

    @Test
    fun `multi-hop pump to pipe to pipe`() {
        val pump = FluidPump(TestHelper.createLocation(0.0, 64.0, 0.0))
        pump.storeFluid(FluidType.WATER)
        val cauldronField = FluidPump::class.java.getDeclaredField("cauldronFace")
        cauldronField.isAccessible = true
        cauldronField.set(pump, BlockFace.NORTH)

        val pipe1 = FluidPipe(TestHelper.createLocation(0.0, 64.0, 1.0), BlockFace.SOUTH)
        val pipe2 = FluidPipe(TestHelper.createLocation(0.0, 64.0, 2.0), BlockFace.SOUTH)

        TestHelper.addToRegistry(fluidRegistry, pump, "fluid_pump")
        TestHelper.addToRegistry(fluidRegistry, pipe1, "fluid_pipe_south")
        TestHelper.addToRegistry(fluidRegistry, pipe2, "fluid_pipe_south")

        // Tick 1: pipe1 pulls from pump
        pipe1.callFluidUpdate()
        assertEquals(FluidType.WATER, pipe1.storedFluid)

        // Tick 1: pipe2 pulls from pipe1
        pipe2.callFluidUpdate()
        assertEquals(FluidType.WATER, pipe2.storedFluid)
        assertEquals(FluidType.NONE, pipe1.storedFluid)
    }
}

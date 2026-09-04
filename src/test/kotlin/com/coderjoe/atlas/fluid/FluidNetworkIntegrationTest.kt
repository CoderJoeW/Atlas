package com.coderjoe.atlas.fluid

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.TestHelper.callFluidUpdate
import com.coderjoe.atlas.fluid.block.FluidContainer
import com.coderjoe.atlas.fluid.block.FluidPipe
import com.coderjoe.atlas.fluid.block.FluidPump
import org.bukkit.block.BlockFace
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * End-to-end behaviour of a pipe run: pipes hold nothing themselves, so every case here is about
 * fluid moving from a provider on the edge of the run to an acceptor on the edge, in one tick and
 * regardless of how long the run is.
 */
class FluidNetworkIntegrationTest {
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

    private fun pipe(
        x: Double,
        y: Double,
        z: Double,
    ): FluidPipe =
        FluidPipe(TestHelper.createLocation(x, y, z)).also {
            TestHelper.addToRegistry(registry, it, "atlas:fluid_pipe")
        }

    /** A pump primed with [fluid] and willing to give it up through [outputFace]. */
    private fun pump(
        x: Double,
        y: Double,
        z: Double,
        fluid: FluidType,
        outputFace: BlockFace,
    ): FluidPump =
        FluidPump(TestHelper.createLocation(x, y, z)).also {
            it.storeFluid(fluid)
            val cauldronField = FluidPump::class.java.getDeclaredField("cauldronFace")
            cauldronField.isAccessible = true
            cauldronField.set(it, outputFace.oppositeFace)
            TestHelper.addToRegistry(registry, it, "atlas:fluid_pump")
        }

    private fun container(
        x: Double,
        y: Double,
        z: Double,
        facing: BlockFace,
    ): FluidContainer =
        FluidContainer(TestHelper.createLocation(x, y, z), facing).also {
            TestHelper.addToRegistry(registry, it, "atlas:fluid_container")
        }

    @Test
    fun `a run carries fluid from a pump to a container in one tick`() {
        val pump = pump(0.0, 64.0, 0.0, FluidType.WATER, BlockFace.SOUTH)
        val run = pipe(0.0, 64.0, 1.0)
        // container at z=2 filling through its back, which faces the pipe at z=1
        val tank = container(0.0, 64.0, 2.0, BlockFace.SOUTH)

        run.callFluidUpdate()

        assertEquals(FluidType.NONE, pump.storedFluid, "pump should have handed its unit over")
        assertEquals(FluidType.WATER, tank.storedFluid)
        assertEquals(1, tank.storedAmount)
    }

    @Test
    fun `length of the run does not matter`() {
        val pump = pump(0.0, 64.0, 0.0, FluidType.LAVA, BlockFace.SOUTH)
        val first = pipe(0.0, 64.0, 1.0)
        pipe(0.0, 64.0, 2.0)
        pipe(0.0, 64.0, 3.0)
        pipe(0.0, 64.0, 4.0)
        val tank = container(0.0, 64.0, 5.0, BlockFace.SOUTH)

        first.callFluidUpdate()

        assertEquals(FluidType.NONE, pump.storedFluid)
        assertEquals(FluidType.LAVA, tank.storedFluid, "one tick should cross the whole run")
    }

    @Test
    fun `a pipe holds nothing of its own`() {
        val run = pipe(0.0, 64.0, 0.0)
        assertEquals(FluidType.NONE, run.storedFluid)
        assertTrue(!run.hasFluid(), "an isolated pipe has no fluid to offer")
    }

    @Test
    fun `a run with no acceptor moves nothing and leaves the pump loaded`() {
        val pump = pump(0.0, 64.0, 0.0, FluidType.WATER, BlockFace.SOUTH)
        val run = pipe(0.0, 64.0, 1.0)

        run.callFluidUpdate()

        assertEquals(FluidType.WATER, pump.storedFluid, "nowhere to send it, so it stays put")
    }

    @Test
    fun `a run reads as carrying whatever its provider is offering`() {
        pump(0.0, 64.0, 0.0, FluidType.LAVA, BlockFace.SOUTH)
        val run = pipe(0.0, 64.0, 1.0)

        run.callFluidUpdate()

        assertEquals(FluidType.LAVA, run.carrying, "the run should glow with the fluid available on it")
    }

    @Test
    fun `a branch reaches an acceptor round the corner`() {
        val pump = pump(0.0, 64.0, 0.0, FluidType.WATER, BlockFace.SOUTH)
        val corner = pipe(0.0, 64.0, 1.0)
        pipe(1.0, 64.0, 1.0)
        val tank = container(2.0, 64.0, 1.0, BlockFace.EAST)

        corner.callFluidUpdate()

        assertEquals(FluidType.NONE, pump.storedFluid)
        assertEquals(FluidType.WATER, tank.storedFluid)
    }
}

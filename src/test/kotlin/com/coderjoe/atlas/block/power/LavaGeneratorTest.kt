package com.coderjoe.atlas.block.power

import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.block.capability.FluidType
import com.coderjoe.atlas.block.fluid.FluidContainer
import com.coderjoe.atlas.block.fluid.FluidPipe
import com.coderjoe.atlas.testing.MockServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Fluid reaches the generator by push now: a pipe's own tick moves a unit from a provider to
 * whichever [com.coderjoe.atlas.block.capability.FluidConsumer] on its edge wants it, so every case here
 * puts a real [FluidPipe] between the tank and the generator and drives the pipe, not the
 * generator, to move fluid. A bare tank touching the generator with no pipe between them is
 * exactly as inert as a battery touching a machine with no cable - see
 * [FluidContainer]'s own class doc.
 */
class LavaGeneratorTest {
    @BeforeEach
    fun setup() {
        MockServer.setup()
    }

    @AfterEach
    fun teardown() {
        MockServer.teardown()
    }

    @Test
    fun `lava generator maxStorage is 20`() {
        val gen = LavaGenerator(MockServer.createLocation())
        assertEquals(20, gen.maxStorage)
    }

    @Test
    fun `lava generator canReceivePower is false`() {
        val gen = LavaGenerator(MockServer.createLocation())
        assertFalse(gen.canAcceptPower())
    }

    @Test
    fun `lava generator visual state idle before it burns anything`() {
        val gen = LavaGenerator(MockServer.createLocation())
        assertEquals(
            "atlas:lava_generator",
            gen.getVisualStateBlockId(),
        )
    }

    @Test
    fun `lava generator visual state active while burning lava`() {
        val registry = BlockRegistry(MockServer.plugin)

        val gen = LavaGenerator(MockServer.createLocation(0.0, 64.0, 0.0))
        registry.track(gen, "atlas:lava_generator")

        val tank = FluidContainer(MockServer.createLocation(0.0, 64.0, -2.0))
        tank.storeFluid(FluidType.LAVA)
        registry.track(tank, "atlas:fluid_container")

        val pipe = FluidPipe(MockServer.createLocation(0.0, 64.0, -1.0))
        registry.track(pipe, "atlas:fluid_pipe")

        pipe.fluidUpdate()
        gen.powerUpdate()

        assertEquals("atlas:lava_generator_active", gen.getVisualStateBlockId())
    }

    @Test
    fun `lava generator goes dark once the lava runs out`() {
        val registry = BlockRegistry(MockServer.plugin)

        val gen = LavaGenerator(MockServer.createLocation(0.0, 64.0, 0.0))
        registry.track(gen, "atlas:lava_generator")

        val tank = FluidContainer(MockServer.createLocation(0.0, 64.0, -2.0))
        tank.storeFluid(FluidType.LAVA)
        registry.track(tank, "atlas:fluid_container")

        val pipe = FluidPipe(MockServer.createLocation(0.0, 64.0, -1.0))
        registry.track(pipe, "atlas:fluid_pipe")

        pipe.fluidUpdate()
        gen.powerUpdate()
        assertEquals("atlas:lava_generator_active", gen.getVisualStateBlockId())

        // the tank is empty now, so the next push moves nothing
        pipe.fluidUpdate()
        gen.powerUpdate()
        assertEquals("atlas:lava_generator", gen.getVisualStateBlockId())
    }

    @Test
    fun `lava generator holding charge but burning nothing reads as idle`() {
        val gen = LavaGenerator(MockServer.createLocation())
        gen.currentPower = gen.maxStorage

        // stored charge is not the same as working - a full generator with no fire is dark
        assertEquals("atlas:lava_generator", gen.getVisualStateBlockId())
    }

    @Test
    fun `lava generator consumes lava pushed through an adjacent fluid pipe`() {
        val registry = BlockRegistry(MockServer.plugin)

        val genLoc = MockServer.createLocation(0.0, 64.0, 0.0)
        val gen = LavaGenerator(genLoc)
        registry.track(gen, "atlas:lava_generator")

        val pipeLoc = MockServer.createLocation(0.0, 64.0, -1.0)
        val pipe = FluidPipe(pipeLoc)
        registry.track(pipe, "atlas:fluid_pipe")

        val tankLoc = MockServer.createLocation(0.0, 64.0, -2.0)
        val tank = FluidContainer(tankLoc)
        tank.storeFluid(FluidType.LAVA)
        registry.track(tank, "atlas:fluid_container")

        pipe.fluidUpdate()

        assertEquals(2, gen.currentPower)
        assertFalse(tank.hasFluid())
    }

    @Test
    fun `lava generator ignores water offered through a fluid pipe`() {
        val registry = BlockRegistry(MockServer.plugin)

        val genLoc = MockServer.createLocation(0.0, 64.0, 0.0)
        val gen = LavaGenerator(genLoc)
        registry.track(gen, "atlas:lava_generator")

        val pipeLoc = MockServer.createLocation(0.0, 64.0, -1.0)
        val pipe = FluidPipe(pipeLoc)
        registry.track(pipe, "atlas:fluid_pipe")

        val tankLoc = MockServer.createLocation(0.0, 64.0, -2.0)
        val tank = FluidContainer(tankLoc)
        tank.storeFluid(FluidType.WATER)
        registry.track(tank, "atlas:fluid_container")

        pipe.fluidUpdate()

        assertEquals(0, gen.currentPower)
        assertTrue(tank.hasFluid())
    }

    @Test
    fun `lava generator takes nothing from an empty tank`() {
        val registry = BlockRegistry(MockServer.plugin)

        val gen = LavaGenerator(MockServer.createLocation(0.0, 64.0, 0.0))
        registry.track(gen, "atlas:lava_generator")

        val pipe = FluidPipe(MockServer.createLocation(0.0, 64.0, -1.0))
        registry.track(pipe, "atlas:fluid_pipe")

        val tank = FluidContainer(MockServer.createLocation(0.0, 64.0, -2.0))
        registry.track(tank, "atlas:fluid_container")

        pipe.fluidUpdate()

        assertEquals(0, gen.currentPower)
    }

    @Test
    fun `lava generator stops consuming when full`() {
        val registry = BlockRegistry(MockServer.plugin)

        val genLoc = MockServer.createLocation(0.0, 64.0, 0.0)
        val gen = LavaGenerator(genLoc)
        gen.currentPower = gen.maxStorage
        registry.track(gen, "atlas:lava_generator")

        val pipe = FluidPipe(MockServer.createLocation(0.0, 64.0, -1.0))
        registry.track(pipe, "atlas:fluid_pipe")

        val tank = FluidContainer(MockServer.createLocation(0.0, 64.0, -2.0))
        tank.storeFluid(FluidType.LAVA)
        registry.track(tank, "atlas:fluid_container")

        pipe.fluidUpdate()

        assertEquals(gen.maxStorage, gen.currentPower)
        assertTrue(tank.hasFluid())
    }

    @Test
    fun `lava generator accepts a push from each of two separate runs`() {
        // one network moves at most one unit per tick (see FluidNetwork.transfer), so "multiple
        // sources in one tick" now means two independent runs each pushing once, not one run
        // serving two providers at once.
        val registry = BlockRegistry(MockServer.plugin)

        val genLoc = MockServer.createLocation(0.0, 64.0, 0.0)
        val gen = LavaGenerator(genLoc)
        registry.track(gen, "atlas:lava_generator")

        val pipe1 = FluidPipe(MockServer.createLocation(0.0, 64.0, -1.0))
        registry.track(pipe1, "atlas:fluid_pipe")
        val tank1 = FluidContainer(MockServer.createLocation(0.0, 64.0, -2.0))
        tank1.storeFluid(FluidType.LAVA)
        registry.track(tank1, "atlas:fluid_container")

        val pipe2 = FluidPipe(MockServer.createLocation(0.0, 64.0, 1.0))
        registry.track(pipe2, "atlas:fluid_pipe")
        val tank2 = FluidContainer(MockServer.createLocation(0.0, 64.0, 2.0))
        tank2.storeFluid(FluidType.LAVA)
        registry.track(tank2, "atlas:fluid_container")

        pipe1.fluidUpdate()
        pipe2.fluidUpdate()

        assertEquals(4, gen.currentPower)
        assertFalse(tank1.hasFluid())
        assertFalse(tank2.hasFluid())
    }

    @Test
    fun `lava generator does nothing when no adjacent fluid blocks`() {
        BlockRegistry(MockServer.plugin)

        val gen = LavaGenerator(MockServer.createLocation())

        gen.powerUpdate()

        assertEquals(0, gen.currentPower)
    }

    @Test
    fun `lava generator descriptor has correct properties`() {
        val desc = LavaGenerator.descriptor
        assertEquals("atlas:lava_generator", desc.baseBlockId)
        assertEquals("Lava Generator", desc.displayName)
        assertEquals(1, desc.additionalBlockIds.size)
        assertTrue(
            desc.additionalBlockIds.contains(
                "atlas:lava_generator_active",
            ),
        )
    }
}

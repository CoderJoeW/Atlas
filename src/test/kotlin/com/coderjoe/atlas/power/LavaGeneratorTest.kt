package com.coderjoe.atlas.power

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.TestHelper.callFluidUpdate
import com.coderjoe.atlas.TestHelper.callPowerUpdate
import com.coderjoe.atlas.fluid.FluidBlockRegistry
import com.coderjoe.atlas.block.capability.FluidType
import com.coderjoe.atlas.fluid.block.FluidContainer
import com.coderjoe.atlas.fluid.block.FluidPipe
import com.coderjoe.atlas.power.block.LavaGenerator
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
 * [com.coderjoe.atlas.fluid.block.FluidContainer]'s own class doc.
 */
class LavaGeneratorTest {
    @BeforeEach
    fun setup() {
        TestHelper.setup()
    }

    @AfterEach
    fun teardown() {
        TestHelper.teardown()
    }

    @Test
    fun `lava generator maxStorage is 20`() {
        val gen = LavaGenerator(TestHelper.createLocation())
        assertEquals(20, gen.maxStorage)
    }

    @Test
    fun `lava generator canReceivePower is false`() {
        val gen = LavaGenerator(TestHelper.createLocation())
        assertFalse(gen.canAcceptPower())
    }

    @Test
    fun `lava generator visual state idle before it burns anything`() {
        val gen = LavaGenerator(TestHelper.createLocation())
        assertEquals(
            "atlas:lava_generator",
            gen.getVisualStateBlockId(),
        )
    }

    @Test
    fun `lava generator visual state active while burning lava`() {
        val fluidRegistry = FluidBlockRegistry(TestHelper.mockPlugin)
        val powerRegistry = PowerBlockRegistry(TestHelper.mockPlugin)

        val gen = LavaGenerator(TestHelper.createLocation(0.0, 64.0, 0.0))
        TestHelper.addToRegistry(powerRegistry, gen, "atlas:lava_generator")

        val tank = FluidContainer(TestHelper.createLocation(0.0, 64.0, -2.0))
        tank.storeFluid(FluidType.LAVA)
        TestHelper.addToRegistry(fluidRegistry, tank, "atlas:fluid_container")

        val pipe = FluidPipe(TestHelper.createLocation(0.0, 64.0, -1.0))
        TestHelper.addToRegistry(fluidRegistry, pipe, "atlas:fluid_pipe")

        pipe.callFluidUpdate()
        gen.callPowerUpdate()

        assertEquals("atlas:lava_generator_active", gen.getVisualStateBlockId())
    }

    @Test
    fun `lava generator goes dark once the lava runs out`() {
        val fluidRegistry = FluidBlockRegistry(TestHelper.mockPlugin)
        val powerRegistry = PowerBlockRegistry(TestHelper.mockPlugin)

        val gen = LavaGenerator(TestHelper.createLocation(0.0, 64.0, 0.0))
        TestHelper.addToRegistry(powerRegistry, gen, "atlas:lava_generator")

        val tank = FluidContainer(TestHelper.createLocation(0.0, 64.0, -2.0))
        tank.storeFluid(FluidType.LAVA)
        TestHelper.addToRegistry(fluidRegistry, tank, "atlas:fluid_container")

        val pipe = FluidPipe(TestHelper.createLocation(0.0, 64.0, -1.0))
        TestHelper.addToRegistry(fluidRegistry, pipe, "atlas:fluid_pipe")

        pipe.callFluidUpdate()
        gen.callPowerUpdate()
        assertEquals("atlas:lava_generator_active", gen.getVisualStateBlockId())

        // the tank is empty now, so the next push moves nothing
        pipe.callFluidUpdate()
        gen.callPowerUpdate()
        assertEquals("atlas:lava_generator", gen.getVisualStateBlockId())
    }

    @Test
    fun `lava generator holding charge but burning nothing reads as idle`() {
        val gen = LavaGenerator(TestHelper.createLocation())
        gen.currentPower = gen.maxStorage

        // stored charge is not the same as working - a full generator with no fire is dark
        assertEquals("atlas:lava_generator", gen.getVisualStateBlockId())
    }

    @Test
    fun `lava generator consumes lava pushed through an adjacent fluid pipe`() {
        val fluidRegistry = FluidBlockRegistry(TestHelper.mockPlugin)
        val powerRegistry = PowerBlockRegistry(TestHelper.mockPlugin)

        val genLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val gen = LavaGenerator(genLoc)
        TestHelper.addToRegistry(powerRegistry, gen, "atlas:lava_generator")

        val pipeLoc = TestHelper.createLocation(0.0, 64.0, -1.0)
        val pipe = FluidPipe(pipeLoc)
        TestHelper.addToRegistry(fluidRegistry, pipe, "atlas:fluid_pipe")

        val tankLoc = TestHelper.createLocation(0.0, 64.0, -2.0)
        val tank = FluidContainer(tankLoc)
        tank.storeFluid(FluidType.LAVA)
        TestHelper.addToRegistry(fluidRegistry, tank, "atlas:fluid_container")

        pipe.callFluidUpdate()

        assertEquals(2, gen.currentPower)
        assertFalse(tank.hasFluid())
    }

    @Test
    fun `lava generator ignores water offered through a fluid pipe`() {
        val fluidRegistry = FluidBlockRegistry(TestHelper.mockPlugin)
        val powerRegistry = PowerBlockRegistry(TestHelper.mockPlugin)

        val genLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val gen = LavaGenerator(genLoc)
        TestHelper.addToRegistry(powerRegistry, gen, "atlas:lava_generator")

        val pipeLoc = TestHelper.createLocation(0.0, 64.0, -1.0)
        val pipe = FluidPipe(pipeLoc)
        TestHelper.addToRegistry(fluidRegistry, pipe, "atlas:fluid_pipe")

        val tankLoc = TestHelper.createLocation(0.0, 64.0, -2.0)
        val tank = FluidContainer(tankLoc)
        tank.storeFluid(FluidType.WATER)
        TestHelper.addToRegistry(fluidRegistry, tank, "atlas:fluid_container")

        pipe.callFluidUpdate()

        assertEquals(0, gen.currentPower)
        assertTrue(tank.hasFluid())
    }

    @Test
    fun `lava generator takes nothing from an empty tank`() {
        val fluidRegistry = FluidBlockRegistry(TestHelper.mockPlugin)
        val powerRegistry = PowerBlockRegistry(TestHelper.mockPlugin)

        val gen = LavaGenerator(TestHelper.createLocation(0.0, 64.0, 0.0))
        TestHelper.addToRegistry(powerRegistry, gen, "atlas:lava_generator")

        val pipe = FluidPipe(TestHelper.createLocation(0.0, 64.0, -1.0))
        TestHelper.addToRegistry(fluidRegistry, pipe, "atlas:fluid_pipe")

        val tank = FluidContainer(TestHelper.createLocation(0.0, 64.0, -2.0))
        TestHelper.addToRegistry(fluidRegistry, tank, "atlas:fluid_container")

        pipe.callFluidUpdate()

        assertEquals(0, gen.currentPower)
    }

    @Test
    fun `lava generator stops consuming when full`() {
        val fluidRegistry = FluidBlockRegistry(TestHelper.mockPlugin)
        val powerRegistry = PowerBlockRegistry(TestHelper.mockPlugin)

        val genLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val gen = LavaGenerator(genLoc)
        gen.currentPower = gen.maxStorage
        TestHelper.addToRegistry(powerRegistry, gen, "atlas:lava_generator")

        val pipe = FluidPipe(TestHelper.createLocation(0.0, 64.0, -1.0))
        TestHelper.addToRegistry(fluidRegistry, pipe, "atlas:fluid_pipe")

        val tank = FluidContainer(TestHelper.createLocation(0.0, 64.0, -2.0))
        tank.storeFluid(FluidType.LAVA)
        TestHelper.addToRegistry(fluidRegistry, tank, "atlas:fluid_container")

        pipe.callFluidUpdate()

        assertEquals(gen.maxStorage, gen.currentPower)
        assertTrue(tank.hasFluid())
    }

    @Test
    fun `lava generator accepts a push from each of two separate runs`() {
        // one network moves at most one unit per tick (see FluidNetwork.transfer), so "multiple
        // sources in one tick" now means two independent runs each pushing once, not one run
        // serving two providers at once.
        val fluidRegistry = FluidBlockRegistry(TestHelper.mockPlugin)
        val powerRegistry = PowerBlockRegistry(TestHelper.mockPlugin)

        val genLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val gen = LavaGenerator(genLoc)
        TestHelper.addToRegistry(powerRegistry, gen, "atlas:lava_generator")

        val pipe1 = FluidPipe(TestHelper.createLocation(0.0, 64.0, -1.0))
        TestHelper.addToRegistry(fluidRegistry, pipe1, "atlas:fluid_pipe")
        val tank1 = FluidContainer(TestHelper.createLocation(0.0, 64.0, -2.0))
        tank1.storeFluid(FluidType.LAVA)
        TestHelper.addToRegistry(fluidRegistry, tank1, "atlas:fluid_container")

        val pipe2 = FluidPipe(TestHelper.createLocation(0.0, 64.0, 1.0))
        TestHelper.addToRegistry(fluidRegistry, pipe2, "atlas:fluid_pipe")
        val tank2 = FluidContainer(TestHelper.createLocation(0.0, 64.0, 2.0))
        tank2.storeFluid(FluidType.LAVA)
        TestHelper.addToRegistry(fluidRegistry, tank2, "atlas:fluid_container")

        pipe1.callFluidUpdate()
        pipe2.callFluidUpdate()

        assertEquals(4, gen.currentPower)
        assertFalse(tank1.hasFluid())
        assertFalse(tank2.hasFluid())
    }

    @Test
    fun `lava generator does nothing when no adjacent fluid blocks`() {
        FluidBlockRegistry(TestHelper.mockPlugin)
        PowerBlockRegistry(TestHelper.mockPlugin)

        val gen = LavaGenerator(TestHelper.createLocation())

        gen.callPowerUpdate()

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

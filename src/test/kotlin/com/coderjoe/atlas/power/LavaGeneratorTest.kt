package com.coderjoe.atlas.power

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.TestHelper.callPowerUpdate
import com.coderjoe.atlas.fluid.FluidBlockRegistry
import com.coderjoe.atlas.fluid.FluidType
import com.coderjoe.atlas.fluid.block.FluidContainer
import com.coderjoe.atlas.fluid.block.FluidPipe
import com.coderjoe.atlas.power.block.LavaGenerator
import org.bukkit.block.BlockFace
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
    fun `lava generator visual state idle when no power`() {
        val gen = LavaGenerator(TestHelper.createLocation())
        assertEquals(
            "atlas:lava_generator",
            gen.getVisualStateBlockId(),
        )
    }

    @Test
    fun `lava generator visual state active when has power`() {
        val gen = LavaGenerator(TestHelper.createLocation())
        gen.currentPower = 5
        assertEquals(
            "atlas:lava_generator_active",
            gen.getVisualStateBlockId(),
        )
    }

    @Test
    fun `lava generator consumes lava from adjacent fluid pipe`() {
        val fluidRegistry = FluidBlockRegistry(TestHelper.mockPlugin)
        val powerRegistry = PowerBlockRegistry(TestHelper.mockPlugin)

        val genLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val gen = LavaGenerator(genLoc)
        TestHelper.addToRegistry(
            powerRegistry,
            gen,
            "atlas:lava_generator",
        )

        val pipeLoc = TestHelper.createLocation(0.0, 64.0, -1.0)
        val pipe = FluidPipe(pipeLoc, BlockFace.SOUTH)
        pipe.storeFluid(FluidType.LAVA)
        TestHelper.addToRegistry(
            fluidRegistry,
            pipe,
            "atlas:fluid_pipe",
        )

        gen.callPowerUpdate()

        assertEquals(5, gen.currentPower)
        assertFalse(pipe.hasFluid())
    }

    @Test
    fun `lava generator ignores water from adjacent fluid pipe`() {
        val fluidRegistry = FluidBlockRegistry(TestHelper.mockPlugin)
        val powerRegistry = PowerBlockRegistry(TestHelper.mockPlugin)

        val genLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val gen = LavaGenerator(genLoc)
        TestHelper.addToRegistry(
            powerRegistry,
            gen,
            "atlas:lava_generator",
        )

        val pipeLoc = TestHelper.createLocation(0.0, 64.0, -1.0)
        val pipe = FluidPipe(pipeLoc, BlockFace.SOUTH)
        pipe.storeFluid(FluidType.WATER)
        TestHelper.addToRegistry(
            fluidRegistry,
            pipe,
            "atlas:fluid_pipe",
        )

        gen.callPowerUpdate()

        assertEquals(0, gen.currentPower)
        assertTrue(pipe.hasFluid())
    }

    @Test
    fun `lava generator consumes lava from fluid container facing toward it`() {
        val fluidRegistry = FluidBlockRegistry(TestHelper.mockPlugin)
        val powerRegistry = PowerBlockRegistry(TestHelper.mockPlugin)

        val genLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val gen = LavaGenerator(genLoc)
        TestHelper.addToRegistry(
            powerRegistry,
            gen,
            "atlas:lava_generator",
        )

        val containerLoc = TestHelper.createLocation(0.0, 64.0, 1.0)
        val container = FluidContainer(containerLoc, BlockFace.NORTH)
        container.restoreState(FluidType.LAVA, 3)
        TestHelper.addToRegistry(
            fluidRegistry,
            container,
            "atlas:fluid_container",
        )

        gen.callPowerUpdate()

        assertEquals(5, gen.currentPower)
        assertEquals(2, container.storedAmount)
    }

    @Test
    fun `lava generator does not consume from container facing away`() {
        val fluidRegistry = FluidBlockRegistry(TestHelper.mockPlugin)
        val powerRegistry = PowerBlockRegistry(TestHelper.mockPlugin)

        val genLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val gen = LavaGenerator(genLoc)
        TestHelper.addToRegistry(
            powerRegistry,
            gen,
            "atlas:lava_generator",
        )

        val containerLoc = TestHelper.createLocation(0.0, 64.0, 1.0)
        val container = FluidContainer(containerLoc, BlockFace.SOUTH)
        container.restoreState(FluidType.LAVA, 3)
        TestHelper.addToRegistry(
            fluidRegistry,
            container,
            "atlas:fluid_container",
        )

        gen.callPowerUpdate()

        assertEquals(0, gen.currentPower)
        assertEquals(3, container.storedAmount)
    }

    @Test
    fun `lava generator stops consuming when full`() {
        val fluidRegistry = FluidBlockRegistry(TestHelper.mockPlugin)
        val powerRegistry = PowerBlockRegistry(TestHelper.mockPlugin)

        val genLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val gen = LavaGenerator(genLoc)
        gen.currentPower = 48
        TestHelper.addToRegistry(
            powerRegistry,
            gen,
            "atlas:lava_generator",
        )

        val pipeLoc = TestHelper.createLocation(0.0, 64.0, -1.0)
        val pipe = FluidPipe(pipeLoc, BlockFace.SOUTH)
        pipe.storeFluid(FluidType.LAVA)
        TestHelper.addToRegistry(
            fluidRegistry,
            pipe,
            "atlas:fluid_pipe",
        )

        gen.callPowerUpdate()

        assertEquals(48, gen.currentPower)
        assertTrue(pipe.hasFluid())
    }

    @Test
    fun `lava generator consumes from multiple sources in one tick`() {
        val fluidRegistry = FluidBlockRegistry(TestHelper.mockPlugin)
        val powerRegistry = PowerBlockRegistry(TestHelper.mockPlugin)

        val genLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val gen = LavaGenerator(genLoc)
        TestHelper.addToRegistry(
            powerRegistry,
            gen,
            "atlas:lava_generator",
        )

        val pipe1Loc = TestHelper.createLocation(0.0, 64.0, -1.0)
        val pipe1 = FluidPipe(pipe1Loc, BlockFace.SOUTH)
        pipe1.storeFluid(FluidType.LAVA)
        TestHelper.addToRegistry(
            fluidRegistry,
            pipe1,
            "atlas:fluid_pipe",
        )

        val pipe2Loc = TestHelper.createLocation(0.0, 64.0, 1.0)
        val pipe2 = FluidPipe(pipe2Loc, BlockFace.NORTH)
        pipe2.storeFluid(FluidType.LAVA)
        TestHelper.addToRegistry(
            fluidRegistry,
            pipe2,
            "atlas:fluid_pipe",
        )

        gen.callPowerUpdate()

        assertEquals(10, gen.currentPower)
        assertFalse(pipe1.hasFluid())
        assertFalse(pipe2.hasFluid())
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
    fun `lava generator does nothing when already at max storage`() {
        val fluidRegistry = FluidBlockRegistry(TestHelper.mockPlugin)
        val powerRegistry = PowerBlockRegistry(TestHelper.mockPlugin)

        val genLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val gen = LavaGenerator(genLoc)
        gen.currentPower = 50
        TestHelper.addToRegistry(
            powerRegistry,
            gen,
            "atlas:lava_generator",
        )

        val pipeLoc = TestHelper.createLocation(0.0, 64.0, -1.0)
        val pipe = FluidPipe(pipeLoc, BlockFace.SOUTH)
        pipe.storeFluid(FluidType.LAVA)
        TestHelper.addToRegistry(
            fluidRegistry,
            pipe,
            "atlas:fluid_pipe",
        )

        gen.callPowerUpdate()

        assertEquals(50, gen.currentPower)
        assertTrue(pipe.hasFluid())
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

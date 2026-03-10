package com.coderjoe.atlas.power

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.TestHelper.callPowerUpdate
import com.coderjoe.atlas.fluid.FluidBlockRegistry
import com.coderjoe.atlas.fluid.FluidType
import com.coderjoe.atlas.fluid.block.FluidContainer
import com.coderjoe.atlas.fluid.block.FluidPipe
import com.coderjoe.atlas.fluid.block.FluidPump
import com.coderjoe.atlas.power.block.LavaGenerator
import org.bukkit.block.BlockFace
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

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
    fun `lava generator maxStorage is 50`() {
        val gen = LavaGenerator(TestHelper.createLocation())
        assertEquals(50, gen.maxStorage)
    }

    @Test
    fun `lava generator canReceivePower is false`() {
        val gen = LavaGenerator(TestHelper.createLocation())
        assertFalse(gen.canAcceptPower())
    }

    @Test
    fun `lava generator visual state idle when no power`() {
        val gen = LavaGenerator(TestHelper.createLocation())
        assertEquals("lava_generator", gen.getVisualStateBlockId())
    }

    @Test
    fun `lava generator visual state active when has power`() {
        val gen = LavaGenerator(TestHelper.createLocation())
        gen.currentPower = 5
        assertEquals("lava_generator_active", gen.getVisualStateBlockId())
    }

    @Test
    fun `lava generator consumes lava from adjacent fluid pipe`() {
        val fluidRegistry = FluidBlockRegistry(TestHelper.mockPlugin)
        val powerRegistry = PowerBlockRegistry(TestHelper.mockPlugin)

        val genLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val gen = LavaGenerator(genLoc)
        TestHelper.addToRegistry(powerRegistry, gen, "lava_generator")

        // Place a lava-filled pipe to the north (z-1)
        val pipeLoc = TestHelper.createLocation(0.0, 64.0, -1.0)
        val pipe = FluidPipe(pipeLoc, BlockFace.SOUTH)
        pipe.storeFluid(FluidType.LAVA)
        TestHelper.addToRegistry(fluidRegistry, pipe, "fluid_pipe_south_filled_lava")

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
        TestHelper.addToRegistry(powerRegistry, gen, "lava_generator")

        val pipeLoc = TestHelper.createLocation(0.0, 64.0, -1.0)
        val pipe = FluidPipe(pipeLoc, BlockFace.SOUTH)
        pipe.storeFluid(FluidType.WATER)
        TestHelper.addToRegistry(fluidRegistry, pipe, "fluid_pipe_south_filled")

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
        TestHelper.addToRegistry(powerRegistry, gen, "lava_generator")

        // Container to the south (z+1) facing north (toward generator)
        val containerLoc = TestHelper.createLocation(0.0, 64.0, 1.0)
        val container = FluidContainer(containerLoc, BlockFace.NORTH)
        container.restoreState(FluidType.LAVA, 3)
        TestHelper.addToRegistry(fluidRegistry, container, "fluid_container_north_lava_low")

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
        TestHelper.addToRegistry(powerRegistry, gen, "lava_generator")

        // Container to the south (z+1) facing south (away from generator)
        val containerLoc = TestHelper.createLocation(0.0, 64.0, 1.0)
        val container = FluidContainer(containerLoc, BlockFace.SOUTH)
        container.restoreState(FluidType.LAVA, 3)
        TestHelper.addToRegistry(fluidRegistry, container, "fluid_container_south_lava_low")

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
        TestHelper.addToRegistry(powerRegistry, gen, "lava_generator")

        // Two lava pipes adjacent - only one should be consumed (48 + 5 = 53 > 50, but 48+5=53, wait no - space is 2, less than 5)
        val pipeLoc = TestHelper.createLocation(0.0, 64.0, -1.0)
        val pipe = FluidPipe(pipeLoc, BlockFace.SOUTH)
        pipe.storeFluid(FluidType.LAVA)
        TestHelper.addToRegistry(fluidRegistry, pipe, "fluid_pipe_south_filled_lava")

        gen.callPowerUpdate()

        // Should NOT consume because space (2) < POWER_PER_LAVA (5)
        assertEquals(48, gen.currentPower)
        assertTrue(pipe.hasFluid())
    }

    @Test
    fun `lava generator consumes from multiple sources in one tick`() {
        val fluidRegistry = FluidBlockRegistry(TestHelper.mockPlugin)
        val powerRegistry = PowerBlockRegistry(TestHelper.mockPlugin)

        val genLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val gen = LavaGenerator(genLoc)
        TestHelper.addToRegistry(powerRegistry, gen, "lava_generator")

        // Pipe to the north
        val pipe1Loc = TestHelper.createLocation(0.0, 64.0, -1.0)
        val pipe1 = FluidPipe(pipe1Loc, BlockFace.SOUTH)
        pipe1.storeFluid(FluidType.LAVA)
        TestHelper.addToRegistry(fluidRegistry, pipe1, "fluid_pipe_south_filled_lava")

        // Pipe to the south
        val pipe2Loc = TestHelper.createLocation(0.0, 64.0, 1.0)
        val pipe2 = FluidPipe(pipe2Loc, BlockFace.NORTH)
        pipe2.storeFluid(FluidType.LAVA)
        TestHelper.addToRegistry(fluidRegistry, pipe2, "fluid_pipe_north_filled_lava")

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
        TestHelper.addToRegistry(powerRegistry, gen, "lava_generator")

        val pipeLoc = TestHelper.createLocation(0.0, 64.0, -1.0)
        val pipe = FluidPipe(pipeLoc, BlockFace.SOUTH)
        pipe.storeFluid(FluidType.LAVA)
        TestHelper.addToRegistry(fluidRegistry, pipe, "fluid_pipe_south_filled_lava")

        gen.callPowerUpdate()

        assertEquals(50, gen.currentPower)
        assertTrue(pipe.hasFluid())
    }

    @Test
    fun `lava generator descriptor has correct properties`() {
        val desc = LavaGenerator.descriptor
        assertEquals("lava_generator", desc.baseBlockId)
        assertEquals("Lava Generator", desc.displayName)
        assertEquals(2, desc.allRegistrableIds.size)
        assertTrue(desc.allRegistrableIds.contains("lava_generator"))
        assertTrue(desc.allRegistrableIds.contains("lava_generator_active"))
    }
}

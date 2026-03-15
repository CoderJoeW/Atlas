package com.coderjoe.atlas.power

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.TestHelper.callPowerUpdate
import com.coderjoe.atlas.fluid.FluidBlockRegistry
import com.coderjoe.atlas.fluid.FluidType
import com.coderjoe.atlas.fluid.block.FluidPipe
import com.coderjoe.atlas.power.block.CobblestoneFactory
import org.bukkit.block.BlockFace
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CobblestoneFactoryTest {
    @BeforeEach
    fun setup() {
        TestHelper.setup()
    }

    @AfterEach
    fun teardown() {
        TestHelper.teardown()
    }

    @Test
    fun `cobblestone generator maxStorage is 2`() {
        val gen = CobblestoneFactory(TestHelper.createLocation())
        assertEquals(2, gen.maxStorage)
    }

    @Test
    fun `cobblestone generator canReceivePower is true`() {
        val gen = CobblestoneFactory(TestHelper.createLocation())
        assertTrue(gen.canAcceptPower())
    }

    @Test
    fun `visual state idle when insufficient power`() {
        val gen = CobblestoneFactory(TestHelper.createLocation())
        assertEquals("cobblestone_factory", gen.getVisualStateBlockId())
    }

    @Test
    fun `visual state active when power at cost`() {
        val gen = CobblestoneFactory(TestHelper.createLocation())
        gen.currentPower = 2
        assertEquals("cobblestone_factory_active", gen.getVisualStateBlockId())
    }

    @Test
    fun `does not generate when only water available`() {
        val fluidRegistry = FluidBlockRegistry(TestHelper.mockPlugin)
        val powerRegistry = PowerBlockRegistry(TestHelper.mockPlugin)

        val genLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val gen = CobblestoneFactory(genLoc)
        gen.currentPower = 2
        TestHelper.addToRegistry(powerRegistry, gen, "cobblestone_factory")

        val pipeLoc = TestHelper.createLocation(0.0, 64.0, -1.0)
        val pipe = FluidPipe(pipeLoc, BlockFace.SOUTH)
        pipe.storeFluid(FluidType.WATER)
        TestHelper.addToRegistry(fluidRegistry, pipe, "fluid_pipe_south_filled_water")

        gen.callPowerUpdate()

        assertEquals(2, gen.currentPower)
        assertTrue(pipe.hasFluid())
    }

    @Test
    fun `does not generate when only lava available`() {
        val fluidRegistry = FluidBlockRegistry(TestHelper.mockPlugin)
        val powerRegistry = PowerBlockRegistry(TestHelper.mockPlugin)

        val genLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val gen = CobblestoneFactory(genLoc)
        gen.currentPower = 2
        TestHelper.addToRegistry(powerRegistry, gen, "cobblestone_factory")

        val pipeLoc = TestHelper.createLocation(0.0, 64.0, -1.0)
        val pipe = FluidPipe(pipeLoc, BlockFace.SOUTH)
        pipe.storeFluid(FluidType.LAVA)
        TestHelper.addToRegistry(fluidRegistry, pipe, "fluid_pipe_south_filled_lava")

        gen.callPowerUpdate()

        assertEquals(2, gen.currentPower)
        assertTrue(pipe.hasFluid())
    }

    @Test
    fun `does not generate when insufficient power`() {
        val fluidRegistry = FluidBlockRegistry(TestHelper.mockPlugin)
        val powerRegistry = PowerBlockRegistry(TestHelper.mockPlugin)

        val genLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val gen = CobblestoneFactory(genLoc)
        gen.currentPower = 1
        TestHelper.addToRegistry(powerRegistry, gen, "cobblestone_factory")

        val waterPipeLoc = TestHelper.createLocation(0.0, 64.0, -1.0)
        val waterPipe = FluidPipe(waterPipeLoc, BlockFace.SOUTH)
        waterPipe.storeFluid(FluidType.WATER)
        TestHelper.addToRegistry(fluidRegistry, waterPipe, "fluid_pipe_south_filled_water")

        val lavaPipeLoc = TestHelper.createLocation(0.0, 64.0, 1.0)
        val lavaPipe = FluidPipe(lavaPipeLoc, BlockFace.NORTH)
        lavaPipe.storeFluid(FluidType.LAVA)
        TestHelper.addToRegistry(fluidRegistry, lavaPipe, "fluid_pipe_north_filled_lava")

        gen.callPowerUpdate()

        assertEquals(1, gen.currentPower)
        assertTrue(waterPipe.hasFluid())
        assertTrue(lavaPipe.hasFluid())
    }

    @Test
    fun `consumes water, lava, and power when all available`() {
        val fluidRegistry = FluidBlockRegistry(TestHelper.mockPlugin)
        val powerRegistry = PowerBlockRegistry(TestHelper.mockPlugin)

        val genLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val gen = CobblestoneFactory(genLoc)
        gen.currentPower = 2
        TestHelper.addToRegistry(powerRegistry, gen, "cobblestone_factory")

        val waterPipeLoc = TestHelper.createLocation(0.0, 64.0, -1.0)
        val waterPipe = FluidPipe(waterPipeLoc, BlockFace.SOUTH)
        waterPipe.storeFluid(FluidType.WATER)
        TestHelper.addToRegistry(fluidRegistry, waterPipe, "fluid_pipe_south_filled_water")

        val lavaPipeLoc = TestHelper.createLocation(0.0, 64.0, 1.0)
        val lavaPipe = FluidPipe(lavaPipeLoc, BlockFace.NORTH)
        lavaPipe.storeFluid(FluidType.LAVA)
        TestHelper.addToRegistry(fluidRegistry, lavaPipe, "fluid_pipe_north_filled_lava")

        try {
            gen.callPowerUpdate()
        } catch (_: Throwable) {
            // ItemStack constructor triggers Registry init in test environment
        }

        assertEquals(0, gen.currentPower)
        assertFalse(waterPipe.hasFluid())
        assertFalse(lavaPipe.hasFluid())
    }

    @Test
    fun `descriptor has correct properties`() {
        val desc = CobblestoneFactory.descriptor
        assertEquals("cobblestone_factory", desc.baseBlockId)
        assertEquals("Cobblestone Factory", desc.displayName)
        assertEquals(2, desc.allRegistrableIds.size)
        assertTrue(desc.allRegistrableIds.contains("cobblestone_factory"))
        assertTrue(desc.allRegistrableIds.contains("cobblestone_factory_active"))
    }

    @Test
    fun `pulls power from adjacent blocks`() {
        FluidBlockRegistry(TestHelper.mockPlugin)
        val powerRegistry = PowerBlockRegistry(TestHelper.mockPlugin)

        val genLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val gen = CobblestoneFactory(genLoc)
        TestHelper.addToRegistry(powerRegistry, gen, "cobblestone_factory")

        val batteryLoc = TestHelper.createLocation(1.0, 64.0, 0.0)
        val battery = com.coderjoe.atlas.power.block.SmallBattery(batteryLoc, BlockFace.DOWN)
        battery.currentPower = 5
        TestHelper.addToRegistry(powerRegistry, battery, "small_battery")

        gen.callPowerUpdate()

        // Pulls 1 power per neighbor per tick
        assertEquals(1, gen.currentPower)
        assertEquals(4, battery.currentPower)
    }
}

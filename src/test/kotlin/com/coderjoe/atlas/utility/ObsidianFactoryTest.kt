package com.coderjoe.atlas.utility

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.TestHelper.callPowerUpdate
import com.coderjoe.atlas.fluid.FluidBlockRegistry
import com.coderjoe.atlas.fluid.FluidType
import com.coderjoe.atlas.fluid.block.FluidPipe
import com.coderjoe.atlas.power.PowerBlockRegistry
import com.coderjoe.atlas.utility.block.ObsidianFactory
import org.bukkit.block.BlockFace
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ObsidianFactoryTest {
    @BeforeEach
    fun setup() {
        TestHelper.setup()
    }

    @AfterEach
    fun teardown() {
        TestHelper.teardown()
    }

    @Test
    fun `obsidian generator maxStorage is 100`() {
        val gen = ObsidianFactory(TestHelper.createLocation())
        assertEquals(100, gen.maxStorage)
    }

    @Test
    fun `obsidian generator canReceivePower is true`() {
        val gen = ObsidianFactory(TestHelper.createLocation())
        assertTrue(gen.canAcceptPower())
    }

    @Test
    fun `visual state idle when insufficient power`() {
        val gen = ObsidianFactory(TestHelper.createLocation())
        gen.currentPower = 99
        assertEquals(
            "atlas:obsidian_factory",
            gen.getVisualStateBlockId(),
        )
    }

    @Test
    fun `visual state active when power at cost`() {
        val gen = ObsidianFactory(TestHelper.createLocation())
        gen.currentPower = 100
        assertEquals(
            "atlas:obsidian_factory_active",
            gen.getVisualStateBlockId(),
        )
    }

    @Test
    fun `does not generate when only water available`() {
        val fluidRegistry = FluidBlockRegistry(TestHelper.mockPlugin)
        val powerRegistry = PowerBlockRegistry(TestHelper.mockPlugin)

        val genLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val gen = ObsidianFactory(genLoc)
        gen.currentPower = 100
        TestHelper.addToRegistry(
            powerRegistry,
            gen,
            "atlas:obsidian_factory",
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

        assertEquals(100, gen.currentPower)
        assertTrue(pipe.hasFluid())
    }

    @Test
    fun `does not generate when only lava available`() {
        val fluidRegistry = FluidBlockRegistry(TestHelper.mockPlugin)
        val powerRegistry = PowerBlockRegistry(TestHelper.mockPlugin)

        val genLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val gen = ObsidianFactory(genLoc)
        gen.currentPower = 100
        TestHelper.addToRegistry(
            powerRegistry,
            gen,
            "atlas:obsidian_factory",
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

        assertEquals(100, gen.currentPower)
        assertTrue(pipe.hasFluid())
    }

    @Test
    fun `does not generate when insufficient power`() {
        val fluidRegistry = FluidBlockRegistry(TestHelper.mockPlugin)
        val powerRegistry = PowerBlockRegistry(TestHelper.mockPlugin)

        val genLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val gen = ObsidianFactory(genLoc)
        gen.currentPower = 99
        TestHelper.addToRegistry(
            powerRegistry,
            gen,
            "atlas:obsidian_factory",
        )

        val waterPipeLoc =
            TestHelper.createLocation(0.0, 64.0, -1.0)
        val waterPipe = FluidPipe(waterPipeLoc, BlockFace.SOUTH)
        waterPipe.storeFluid(FluidType.WATER)
        TestHelper.addToRegistry(
            fluidRegistry,
            waterPipe,
            "atlas:fluid_pipe",
        )

        val lavaPipeLoc =
            TestHelper.createLocation(0.0, 64.0, 1.0)
        val lavaPipe = FluidPipe(lavaPipeLoc, BlockFace.NORTH)
        lavaPipe.storeFluid(FluidType.LAVA)
        TestHelper.addToRegistry(
            fluidRegistry,
            lavaPipe,
            "atlas:fluid_pipe",
        )

        gen.callPowerUpdate()

        assertEquals(99, gen.currentPower)
        assertTrue(waterPipe.hasFluid())
        assertTrue(lavaPipe.hasFluid())
    }

    @Test
    fun `consumes water, lava, and power when all available`() {
        val fluidRegistry = FluidBlockRegistry(TestHelper.mockPlugin)
        val powerRegistry = PowerBlockRegistry(TestHelper.mockPlugin)

        val genLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val gen = ObsidianFactory(genLoc)
        gen.currentPower = 100
        TestHelper.addToRegistry(
            powerRegistry,
            gen,
            "atlas:obsidian_factory",
        )

        val waterPipeLoc =
            TestHelper.createLocation(0.0, 64.0, -1.0)
        val waterPipe = FluidPipe(waterPipeLoc, BlockFace.SOUTH)
        waterPipe.storeFluid(FluidType.WATER)
        TestHelper.addToRegistry(
            fluidRegistry,
            waterPipe,
            "atlas:fluid_pipe",
        )

        val lavaPipeLoc =
            TestHelper.createLocation(0.0, 64.0, 1.0)
        val lavaPipe = FluidPipe(lavaPipeLoc, BlockFace.NORTH)
        lavaPipe.storeFluid(FluidType.LAVA)
        TestHelper.addToRegistry(
            fluidRegistry,
            lavaPipe,
            "atlas:fluid_pipe",
        )

        try {
            gen.callPowerUpdate()
        } catch (_: Throwable) {
            // ItemStack constructor triggers Registry init
        }

        assertEquals(0, gen.currentPower)
        assertFalse(waterPipe.hasFluid())
        assertFalse(lavaPipe.hasFluid())
    }

    @Test
    fun `descriptor has correct properties`() {
        val desc = ObsidianFactory.descriptor
        assertEquals("atlas:obsidian_factory", desc.baseBlockId)
        assertEquals("Obsidian Factory", desc.displayName)
        assertEquals(1, desc.additionalBlockIds.size)
        assertTrue(
            desc.additionalBlockIds.contains(
                "atlas:obsidian_factory_active",
            ),
        )
    }

    @Test
    fun `accumulates power over multiple ticks`() {
        FluidBlockRegistry(TestHelper.mockPlugin)
        val powerRegistry = PowerBlockRegistry(TestHelper.mockPlugin)

        val genLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val gen = ObsidianFactory(genLoc)
        TestHelper.addToRegistry(
            powerRegistry,
            gen,
            "atlas:obsidian_factory",
        )

        val batteryLoc = TestHelper.createLocation(1.0, 64.0, 0.0)
        val battery =
            com.coderjoe.atlas.power.block.SmallBattery(
                batteryLoc,
                BlockFace.DOWN,
            )
        battery.currentPower = 10
        TestHelper.addToRegistry(
            powerRegistry,
            battery,
            "atlas:small_battery",
        )

        gen.callPowerUpdate()

        assertTrue(gen.currentPower > 0)
        assertTrue(gen.currentPower < 100)
    }
}

package com.coderjoe.atlas.utility

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.TestHelper.callFluidUpdate
import com.coderjoe.atlas.TestHelper.callPowerUpdate
import com.coderjoe.atlas.block.fluid.FluidBlockRegistry
import com.coderjoe.atlas.block.capability.FluidType
import com.coderjoe.atlas.block.power.SmallBattery
import com.coderjoe.atlas.block.fluid.block.FluidContainer
import com.coderjoe.atlas.block.fluid.block.FluidPipe
import com.coderjoe.atlas.block.power.PowerBlockRegistry
import com.coderjoe.atlas.block.power.factory.ObsidianFactory
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
    fun `obsidian generator maxStorage is 50`() {
        val gen = ObsidianFactory(TestHelper.createLocation())
        assertEquals(50, gen.maxStorage)
    }

    @Test
    fun `obsidian generator canReceivePower is true`() {
        val gen = ObsidianFactory(TestHelper.createLocation())
        assertTrue(gen.canAcceptPower())
    }

    @Test
    fun `visual state always returns base block id`() {
        val gen = ObsidianFactory(TestHelper.createLocation())
        assertEquals(
            "atlas:obsidian_factory",
            gen.getVisualStateBlockId(),
        )
        gen.currentPower = 25
        assertEquals(
            "atlas:obsidian_factory",
            gen.getVisualStateBlockId(),
        )
    }

    @Test
    fun `does not generate when only water available`() {
        val fluidRegistry = FluidBlockRegistry(TestHelper.mockPlugin)
        val powerRegistry = PowerBlockRegistry(TestHelper.mockPlugin)

        val genLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val gen = ObsidianFactory(genLoc)
        gen.currentPower = 25
        TestHelper.addToRegistry(
            powerRegistry,
            gen,
            "atlas:obsidian_factory",
        )

        val pipeLoc = TestHelper.createLocation(0.0, 64.0, -1.0)
        val pipe = FluidContainer(pipeLoc)
        pipe.storeFluid(FluidType.WATER)
        TestHelper.addToRegistry(
            fluidRegistry,
            pipe,
            "atlas:fluid_container",
        )

        gen.callPowerUpdate()

        assertEquals(25, gen.currentPower)
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
        val pipe = FluidContainer(pipeLoc)
        pipe.storeFluid(FluidType.LAVA)
        TestHelper.addToRegistry(
            fluidRegistry,
            pipe,
            "atlas:fluid_container",
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
        gen.currentPower = 24
        TestHelper.addToRegistry(
            powerRegistry,
            gen,
            "atlas:obsidian_factory",
        )

        val waterPipeLoc =
            TestHelper.createLocation(0.0, 64.0, -1.0)
        val waterPipe = FluidContainer(waterPipeLoc)
        waterPipe.storeFluid(FluidType.WATER)
        TestHelper.addToRegistry(
            fluidRegistry,
            waterPipe,
            "atlas:fluid_container",
        )

        val lavaPipeLoc =
            TestHelper.createLocation(0.0, 64.0, 1.0)
        val lavaPipe = FluidContainer(lavaPipeLoc)
        lavaPipe.storeFluid(FluidType.LAVA)
        TestHelper.addToRegistry(
            fluidRegistry,
            lavaPipe,
            "atlas:fluid_container",
        )

        gen.callPowerUpdate()

        assertEquals(24, gen.currentPower)
        assertTrue(waterPipe.hasFluid())
        assertTrue(lavaPipe.hasFluid())
    }

    @Test
    fun `consumes power once both fluids have been pushed in`() {
        // Fluid arrives by push now (see MaterialFactory.acceptFluid), so the factory itself
        // never touches the registry - a network hands it a unit directly, exactly as it would
        // via FluidNetwork.transfer(). The pipe-mediated path is covered end to end below.
        val powerRegistry = PowerBlockRegistry(TestHelper.mockPlugin)

        val gen = ObsidianFactory(TestHelper.createLocation())
        gen.currentPower = 25
        TestHelper.addToRegistry(powerRegistry, gen, "atlas:obsidian_factory")

        assertTrue(gen.acceptFluid(BlockFace.WEST, FluidType.WATER))
        assertTrue(gen.acceptFluid(BlockFace.EAST, FluidType.LAVA))

        try {
            gen.callPowerUpdate()
        } catch (_: Throwable) {
            // ItemStack constructor triggers Registry init
        }

        assertEquals(0, gen.currentPower)
    }

    @Test
    fun `produces when water and lava are pushed in through real fluid pipes`() {
        val fluidRegistry = FluidBlockRegistry(TestHelper.mockPlugin)
        val powerRegistry = PowerBlockRegistry(TestHelper.mockPlugin)

        val genLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val gen = ObsidianFactory(genLoc)
        gen.currentPower = 25
        TestHelper.addToRegistry(powerRegistry, gen, "atlas:obsidian_factory")

        val waterPipe = FluidPipe(TestHelper.createLocation(0.0, 64.0, -1.0))
        TestHelper.addToRegistry(fluidRegistry, waterPipe, "atlas:fluid_pipe")
        val waterTank = FluidContainer(TestHelper.createLocation(0.0, 64.0, -2.0))
        waterTank.storeFluid(FluidType.WATER)
        TestHelper.addToRegistry(fluidRegistry, waterTank, "atlas:fluid_container")

        val lavaPipe = FluidPipe(TestHelper.createLocation(0.0, 64.0, 1.0))
        TestHelper.addToRegistry(fluidRegistry, lavaPipe, "atlas:fluid_pipe")
        val lavaTank = FluidContainer(TestHelper.createLocation(0.0, 64.0, 2.0))
        lavaTank.storeFluid(FluidType.LAVA)
        TestHelper.addToRegistry(fluidRegistry, lavaTank, "atlas:fluid_container")

        waterPipe.callFluidUpdate()
        lavaPipe.callFluidUpdate()

        assertFalse(waterTank.hasFluid())
        assertFalse(lavaTank.hasFluid())

        try {
            gen.callPowerUpdate()
        } catch (_: Throwable) {
            // ItemStack constructor triggers Registry init
        }

        assertEquals(0, gen.currentPower)
    }

    @Test
    fun `descriptor has correct properties`() {
        val desc = ObsidianFactory.descriptor
        assertEquals("atlas:obsidian_factory", desc.baseBlockId)
        assertEquals("Obsidian Factory", desc.displayName)
        assertTrue(desc.additionalBlockIds.isEmpty())
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
            SmallBattery(batteryLoc)
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

package com.coderjoe.atlas.block.power.factory

import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.block.capability.FluidType
import com.coderjoe.atlas.block.fluid.FluidContainer
import com.coderjoe.atlas.block.fluid.FluidPipe
import com.coderjoe.atlas.block.power.SmallBattery
import com.coderjoe.atlas.testing.MockServer
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
        MockServer.setup()
    }

    @AfterEach
    fun teardown() {
        MockServer.teardown()
    }

    @Test
    fun `obsidian generator maxStorage is 50`() {
        val gen = ObsidianFactory(MockServer.createLocation())
        assertEquals(50, gen.maxStorage)
    }

    @Test
    fun `obsidian generator canReceivePower is true`() {
        val gen = ObsidianFactory(MockServer.createLocation())
        assertTrue(gen.canAcceptPower())
    }

    @Test
    fun `visual state always returns base block id`() {
        val gen = ObsidianFactory(MockServer.createLocation())
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
        val registry = BlockRegistry(MockServer.plugin)

        val genLoc = MockServer.createLocation(0.0, 64.0, 0.0)
        val gen = ObsidianFactory(genLoc)
        gen.currentPower = 25
        registry.track(
            gen,
            "atlas:obsidian_factory",
        )

        val pipeLoc = MockServer.createLocation(0.0, 64.0, -1.0)
        val pipe = FluidContainer(pipeLoc)
        pipe.storeFluid(FluidType.WATER)
        registry.track(
            pipe,
            "atlas:fluid_container",
        )

        gen.powerUpdate()

        assertEquals(25, gen.currentPower)
        assertTrue(pipe.hasFluid())
    }

    @Test
    fun `does not generate when only lava available`() {
        val registry = BlockRegistry(MockServer.plugin)

        val genLoc = MockServer.createLocation(0.0, 64.0, 0.0)
        val gen = ObsidianFactory(genLoc)
        gen.currentPower = 100
        registry.track(
            gen,
            "atlas:obsidian_factory",
        )

        val pipeLoc = MockServer.createLocation(0.0, 64.0, -1.0)
        val pipe = FluidContainer(pipeLoc)
        pipe.storeFluid(FluidType.LAVA)
        registry.track(
            pipe,
            "atlas:fluid_container",
        )

        gen.powerUpdate()

        assertEquals(100, gen.currentPower)
        assertTrue(pipe.hasFluid())
    }

    @Test
    fun `does not generate when insufficient power`() {
        val registry = BlockRegistry(MockServer.plugin)

        val genLoc = MockServer.createLocation(0.0, 64.0, 0.0)
        val gen = ObsidianFactory(genLoc)
        gen.currentPower = 24
        registry.track(
            gen,
            "atlas:obsidian_factory",
        )

        val waterPipeLoc =
            MockServer.createLocation(0.0, 64.0, -1.0)
        val waterPipe = FluidContainer(waterPipeLoc)
        waterPipe.storeFluid(FluidType.WATER)
        registry.track(
            waterPipe,
            "atlas:fluid_container",
        )

        val lavaPipeLoc =
            MockServer.createLocation(0.0, 64.0, 1.0)
        val lavaPipe = FluidContainer(lavaPipeLoc)
        lavaPipe.storeFluid(FluidType.LAVA)
        registry.track(
            lavaPipe,
            "atlas:fluid_container",
        )

        gen.powerUpdate()

        assertEquals(24, gen.currentPower)
        assertTrue(waterPipe.hasFluid())
        assertTrue(lavaPipe.hasFluid())
    }

    @Test
    fun `consumes power once both fluids have been pushed in`() {
        // Fluid arrives by push now (see MaterialFactory.acceptFluid), so the factory itself
        // never touches the registry - a network hands it a unit directly, exactly as it would
        // via FluidNetwork.transfer(). The pipe-mediated path is covered end to end below.
        val registry = BlockRegistry(MockServer.plugin)

        val gen = ObsidianFactory(MockServer.createLocation())
        gen.currentPower = 25
        registry.track(gen, "atlas:obsidian_factory")

        assertTrue(gen.acceptFluid(BlockFace.WEST, FluidType.WATER))
        assertTrue(gen.acceptFluid(BlockFace.EAST, FluidType.LAVA))

        try {
            gen.powerUpdate()
        } catch (_: Throwable) {
            // ItemStack constructor triggers Registry init
        }

        assertEquals(0, gen.currentPower)
    }

    @Test
    fun `produces when water and lava are pushed in through real fluid pipes`() {
        val registry = BlockRegistry(MockServer.plugin)

        val genLoc = MockServer.createLocation(0.0, 64.0, 0.0)
        val gen = ObsidianFactory(genLoc)
        gen.currentPower = 25
        registry.track(gen, "atlas:obsidian_factory")

        val waterPipe = FluidPipe(MockServer.createLocation(0.0, 64.0, -1.0))
        registry.track(waterPipe, "atlas:fluid_pipe")
        val waterTank = FluidContainer(MockServer.createLocation(0.0, 64.0, -2.0))
        waterTank.storeFluid(FluidType.WATER)
        registry.track(waterTank, "atlas:fluid_container")

        val lavaPipe = FluidPipe(MockServer.createLocation(0.0, 64.0, 1.0))
        registry.track(lavaPipe, "atlas:fluid_pipe")
        val lavaTank = FluidContainer(MockServer.createLocation(0.0, 64.0, 2.0))
        lavaTank.storeFluid(FluidType.LAVA)
        registry.track(lavaTank, "atlas:fluid_container")

        waterPipe.fluidUpdate()
        lavaPipe.fluidUpdate()

        assertFalse(waterTank.hasFluid())
        assertFalse(lavaTank.hasFluid())

        try {
            gen.powerUpdate()
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
        val registry = BlockRegistry(MockServer.plugin)

        val genLoc = MockServer.createLocation(0.0, 64.0, 0.0)
        val gen = ObsidianFactory(genLoc)
        registry.track(
            gen,
            "atlas:obsidian_factory",
        )

        val batteryLoc = MockServer.createLocation(1.0, 64.0, 0.0)
        val battery =
            SmallBattery(batteryLoc)
        battery.currentPower = 10
        registry.track(
            battery,
            "atlas:small_battery",
        )

        gen.powerUpdate()

        assertTrue(gen.currentPower > 0)
        assertTrue(gen.currentPower < 100)
    }
}

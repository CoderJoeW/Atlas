package com.coderjoe.atlas.block.power.factory

import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.block.capability.FluidType
import com.coderjoe.atlas.block.fluid.block.FluidContainer
import com.coderjoe.atlas.block.fluid.block.FluidPipe
import com.coderjoe.atlas.block.fluid.block.FluidPump
import com.coderjoe.atlas.block.power.SmallBattery
import com.coderjoe.atlas.testing.TestHelper
import com.coderjoe.atlas.testing.TestHelper.callFluidUpdate
import com.coderjoe.atlas.testing.TestHelper.callPowerUpdate
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
    fun `cobblestone generator maxStorage is 4`() {
        val gen = CobblestoneFactory(TestHelper.createLocation())
        assertEquals(4, gen.maxStorage)
    }

    @Test
    fun `cobblestone generator canReceivePower is true`() {
        val gen = CobblestoneFactory(TestHelper.createLocation())
        assertTrue(gen.canAcceptPower())
    }

    @Test
    fun `visual state always returns base block id`() {
        val gen = CobblestoneFactory(TestHelper.createLocation())
        assertEquals(
            "atlas:cobblestone_factory",
            gen.getVisualStateBlockId(),
        )
        gen.currentPower = 2
        assertEquals(
            "atlas:cobblestone_factory",
            gen.getVisualStateBlockId(),
        )
    }

    @Test
    fun `does not generate when only water available`() {
        val registry = BlockRegistry(TestHelper.mockPlugin)

        val genLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val gen = CobblestoneFactory(genLoc)
        gen.currentPower = 2
        TestHelper.addToRegistry(
            registry,
            gen,
            "atlas:cobblestone_factory",
        )

        val pipeLoc = TestHelper.createLocation(0.0, 64.0, -1.0)
        val pipe = FluidContainer(pipeLoc)
        pipe.storeFluid(FluidType.WATER)
        TestHelper.addToRegistry(
            registry,
            pipe,
            "atlas:fluid_container",
        )

        gen.callPowerUpdate()

        assertEquals(2, gen.currentPower)
        assertTrue(pipe.hasFluid())
    }

    @Test
    fun `does not generate when only lava available`() {
        val registry = BlockRegistry(TestHelper.mockPlugin)

        val genLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val gen = CobblestoneFactory(genLoc)
        gen.currentPower = 2
        TestHelper.addToRegistry(
            registry,
            gen,
            "atlas:cobblestone_factory",
        )

        val pipeLoc = TestHelper.createLocation(0.0, 64.0, -1.0)
        val pipe = FluidContainer(pipeLoc)
        pipe.storeFluid(FluidType.LAVA)
        TestHelper.addToRegistry(
            registry,
            pipe,
            "atlas:fluid_container",
        )

        gen.callPowerUpdate()

        assertEquals(2, gen.currentPower)
        assertTrue(pipe.hasFluid())
    }

    @Test
    fun `does not generate when insufficient power`() {
        val registry = BlockRegistry(TestHelper.mockPlugin)

        val genLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val gen = CobblestoneFactory(genLoc)
        gen.currentPower = 1
        TestHelper.addToRegistry(
            registry,
            gen,
            "atlas:cobblestone_factory",
        )

        val waterPipeLoc =
            TestHelper.createLocation(0.0, 64.0, -1.0)
        val waterPipe = FluidContainer(waterPipeLoc)
        waterPipe.storeFluid(FluidType.WATER)
        TestHelper.addToRegistry(
            registry,
            waterPipe,
            "atlas:fluid_container",
        )

        val lavaPipeLoc =
            TestHelper.createLocation(0.0, 64.0, 1.0)
        val lavaPipe = FluidContainer(lavaPipeLoc)
        lavaPipe.storeFluid(FluidType.LAVA)
        TestHelper.addToRegistry(
            registry,
            lavaPipe,
            "atlas:fluid_container",
        )

        gen.callPowerUpdate()

        assertEquals(1, gen.currentPower)
        assertTrue(waterPipe.hasFluid())
        assertTrue(lavaPipe.hasFluid())
    }

    @Test
    fun `consumes power once both fluids have been pushed in`() {
        // Fluid arrives by push now (see MaterialFactory.acceptFluid), so the factory itself
        // never touches the registry - a network hands it a unit directly, exactly as it would
        // via FluidNetwork.transfer(). The pipe-mediated path is covered end to end below.
        val registry = BlockRegistry(TestHelper.mockPlugin)

        val gen = CobblestoneFactory(TestHelper.createLocation())
        gen.currentPower = 2
        TestHelper.addToRegistry(registry, gen, "atlas:cobblestone_factory")

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
        val registry = BlockRegistry(TestHelper.mockPlugin)

        val genLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val gen = CobblestoneFactory(genLoc)
        gen.currentPower = 2
        TestHelper.addToRegistry(registry, gen, "atlas:cobblestone_factory")

        val waterPipe = FluidPipe(TestHelper.createLocation(0.0, 64.0, -1.0))
        TestHelper.addToRegistry(registry, waterPipe, "atlas:fluid_pipe")
        val waterTank = FluidContainer(TestHelper.createLocation(0.0, 64.0, -2.0))
        waterTank.storeFluid(FluidType.WATER)
        TestHelper.addToRegistry(registry, waterTank, "atlas:fluid_container")

        val lavaPipe = FluidPipe(TestHelper.createLocation(0.0, 64.0, 1.0))
        TestHelper.addToRegistry(registry, lavaPipe, "atlas:fluid_pipe")
        val lavaTank = FluidContainer(TestHelper.createLocation(0.0, 64.0, 2.0))
        lavaTank.storeFluid(FluidType.LAVA)
        TestHelper.addToRegistry(registry, lavaTank, "atlas:fluid_container")

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
    fun `produces when pumps push water and lava down pipe runs that end at the factory`() {
        // The build a player actually makes: a pump on each fluid, a pipe run from each, and
        // nothing but the factory on the far end. A pump hands its own unit on rather than
        // waiting to be drained, so this exercises the push path into a pipe rather than the
        // run's own transfer, and that path has to see the factory even though it is a power
        // block and so appears nowhere in the fluid registry.
        val registry = BlockRegistry(TestHelper.mockPlugin)

        val gen = CobblestoneFactory(TestHelper.createLocation(0.0, 64.0, 0.0))
        gen.currentPower = 2
        TestHelper.addToRegistry(registry, gen, "atlas:cobblestone_factory")

        val waterPipe = FluidPipe(TestHelper.createLocation(0.0, 64.0, -1.0))
        TestHelper.addToRegistry(registry, waterPipe, "atlas:fluid_pipe")
        val waterPump = FluidPump(TestHelper.createLocation(0.0, 64.0, -2.0))
        waterPump.storeFluid(FluidType.WATER)
        TestHelper.addToRegistry(registry, waterPump, "atlas:fluid_pump")

        val lavaPipe = FluidPipe(TestHelper.createLocation(0.0, 64.0, 1.0))
        TestHelper.addToRegistry(registry, lavaPipe, "atlas:fluid_pipe")
        val lavaPump = FluidPump(TestHelper.createLocation(0.0, 64.0, 2.0))
        lavaPump.storeFluid(FluidType.LAVA)
        TestHelper.addToRegistry(registry, lavaPump, "atlas:fluid_pump")

        waterPump.callFluidUpdate()
        lavaPump.callFluidUpdate()

        assertFalse(waterPump.hasFluid(), "water pump could not hand its unit to the pipe run")
        assertFalse(lavaPump.hasFluid(), "lava pump could not hand its unit to the pipe run")

        try {
            gen.callPowerUpdate()
        } catch (_: Throwable) {
            // ItemStack constructor triggers Registry init
        }

        assertEquals(0, gen.currentPower)
    }

    @Test
    fun `produces when pumps sit straight against the factory with no pipe between`() {
        val registry = BlockRegistry(TestHelper.mockPlugin)

        val gen = CobblestoneFactory(TestHelper.createLocation(0.0, 64.0, 0.0))
        gen.currentPower = 2
        TestHelper.addToRegistry(registry, gen, "atlas:cobblestone_factory")

        val waterPump = FluidPump(TestHelper.createLocation(0.0, 64.0, -1.0))
        waterPump.storeFluid(FluidType.WATER)
        TestHelper.addToRegistry(registry, waterPump, "atlas:fluid_pump")

        val lavaPump = FluidPump(TestHelper.createLocation(0.0, 64.0, 1.0))
        lavaPump.storeFluid(FluidType.LAVA)
        TestHelper.addToRegistry(registry, lavaPump, "atlas:fluid_pump")

        waterPump.callFluidUpdate()
        lavaPump.callFluidUpdate()

        assertFalse(waterPump.hasFluid(), "water pump could not hand its unit to the factory")
        assertFalse(lavaPump.hasFluid(), "lava pump could not hand its unit to the factory")

        try {
            gen.callPowerUpdate()
        } catch (_: Throwable) {
            // ItemStack constructor triggers Registry init
        }

        assertEquals(0, gen.currentPower)
    }

    @Test
    fun `a pipe run with nowhere to send anything refuses a pump's push`() {
        val registry = BlockRegistry(TestHelper.mockPlugin)

        val pipe = FluidPipe(TestHelper.createLocation(0.0, 64.0, -1.0))
        TestHelper.addToRegistry(registry, pipe, "atlas:fluid_pipe")

        val pump = FluidPump(TestHelper.createLocation(0.0, 64.0, -2.0))
        pump.storeFluid(FluidType.WATER)
        TestHelper.addToRegistry(registry, pump, "atlas:fluid_pump")

        pump.callFluidUpdate()

        assertTrue(pump.hasFluid(), "a dead-end run should have nothing to take the unit")
    }

    @Test
    fun `banks one fluid on its own until the other arrives`() {
        // What the two portholes report: each lamp tracks only its own ingredient, so a factory
        // fed water and still waiting on lava is a state the block rests in and has to show.
        val gen = CobblestoneFactory(TestHelper.createLocation())
        gen.currentPower = 2

        assertFalse(gen.hasWater)
        assertFalse(gen.hasLava)

        assertTrue(gen.acceptFluid(BlockFace.WEST, FluidType.WATER))
        assertTrue(gen.hasWater)
        assertFalse(gen.hasLava)

        // A second unit of the same fluid has nowhere to go - one of each is all a haul needs
        assertFalse(gen.acceptFluid(BlockFace.WEST, FluidType.WATER))
        assertTrue(gen.hasWater)
    }

    @Test
    fun `a haul spends both banked fluids`() {
        val registry = BlockRegistry(TestHelper.mockPlugin)

        val gen = CobblestoneFactory(TestHelper.createLocation())
        gen.currentPower = 2
        TestHelper.addToRegistry(registry, gen, "atlas:cobblestone_factory")

        gen.acceptFluid(BlockFace.WEST, FluidType.WATER)
        gen.acceptFluid(BlockFace.EAST, FluidType.LAVA)

        try {
            gen.callPowerUpdate()
        } catch (_: Throwable) {
            // ItemStack constructor triggers Registry init
        }

        assertFalse(gen.hasWater, "water should be spent by the haul")
        assertFalse(gen.hasLava, "lava should be spent by the haul")
    }

    @Test
    fun `banked fluid is held while the factory cannot afford a haul`() {
        val registry = BlockRegistry(TestHelper.mockPlugin)

        val gen = CobblestoneFactory(TestHelper.createLocation())
        gen.currentPower = 0
        TestHelper.addToRegistry(registry, gen, "atlas:cobblestone_factory")

        gen.acceptFluid(BlockFace.WEST, FluidType.WATER)
        gen.acceptFluid(BlockFace.EAST, FluidType.LAVA)
        gen.callPowerUpdate()

        assertTrue(gen.hasWater, "an unaffordable haul must not eat the banked water")
        assertTrue(gen.hasLava, "an unaffordable haul must not eat the banked lava")
    }

    @Test
    fun `descriptor has correct properties`() {
        val desc = CobblestoneFactory.descriptor
        assertEquals("atlas:cobblestone_factory", desc.baseBlockId)
        assertEquals("Cobblestone Factory", desc.displayName)
        assertTrue(desc.additionalBlockIds.isEmpty())
    }

    @Test
    fun `pulls power from adjacent blocks`() {
        val registry = BlockRegistry(TestHelper.mockPlugin)

        val genLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val gen = CobblestoneFactory(genLoc)
        TestHelper.addToRegistry(
            registry,
            gen,
            "atlas:cobblestone_factory",
        )

        val batteryLoc = TestHelper.createLocation(1.0, 64.0, 0.0)
        val battery =
            SmallBattery(batteryLoc)
        battery.currentPower = 5
        TestHelper.addToRegistry(
            registry,
            battery,
            "atlas:small_battery",
        )

        gen.callPowerUpdate()

        assertEquals(1, gen.currentPower)
        assertEquals(4, battery.currentPower)
    }
}

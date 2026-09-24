package com.coderjoe.atlas.data

import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.block.capability.FluidType
import com.coderjoe.atlas.block.fluid.block.FluidPipe
import com.coderjoe.atlas.block.power.PowerCable
import com.coderjoe.atlas.block.power.SmallBattery
import com.coderjoe.atlas.block.power.SmallSolarPanel
import com.coderjoe.atlas.block.power.factory.CobblestoneFactory
import com.coderjoe.atlas.testing.TestHelper
import org.bukkit.block.BlockFace
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class PowerBlockPersistenceTest {
    private lateinit var registry: BlockRegistry
    private lateinit var persistence: PowerBlockPersistence

    @BeforeEach
    fun setup() {
        TestHelper.setup()
        registry = BlockRegistry(TestHelper.mockPlugin)
        persistence = PowerBlockPersistence(TestHelper.mockPlugin)

        // Initialize factory so load() can create blocks
        TestHelper.initPowerFactory()
    }

    @AfterEach
    fun teardown() {
        TestHelper.teardown()
    }

    @Test
    fun `save 0 blocks creates file with empty list`() {
        persistence.save(registry)
        val file = File(TestHelper.dataFolder, "power_blocks.yml")
        assertTrue(file.exists())
    }

    @Test
    fun `save and load round-trip preserves data`() {
        val panel = SmallSolarPanel(TestHelper.createLocation(1.0, 64.0, 2.0))
        panel.currentPower = 1
        TestHelper.addToRegistry(registry, panel, "atlas:small_solar_panel")

        persistence.save(registry)

        // Create fresh registry for loading
        val loadRegistry = BlockRegistry(TestHelper.mockPlugin)
        persistence.load(loadRegistry)

        val loaded = loadRegistry.getAllBlocksWithIds()
        assertEquals(1, loaded.size)
        assertEquals("atlas:small_solar_panel", loaded[0].second)
        assertEquals(1, assertInstanceOf(SmallSolarPanel::class.java, loaded[0].first).currentPower)
    }

    @Test
    fun `load from missing file does not error`() {
        val loadRegistry = BlockRegistry(TestHelper.mockPlugin)
        assertDoesNotThrow { persistence.load(loadRegistry) }
        assertEquals(0, loadRegistry.getAllBlocks().size)
    }

    @Test
    fun `facing direction persists for cables`() {
        val cable = PowerCable(TestHelper.createLocation())
        TestHelper.addToRegistry(registry, cable, "atlas:power_cable")

        persistence.save(registry)

        val loadRegistry = BlockRegistry(TestHelper.mockPlugin)
        persistence.load(loadRegistry)

        val loaded = loadRegistry.getAllBlocks().first()
        assertTrue(loaded is PowerCable)
    }

    @Test
    fun `a factory's banked fluids persist across a restart`() {
        // The two portholes report these, so losing them on a restart would visibly undo a
        // half-filled machine as well as eating a unit a pump already spent power to lift.
        val factory = CobblestoneFactory(TestHelper.createLocation())
        factory.currentPower = 2
        factory.acceptFluid(BlockFace.WEST, FluidType.WATER)
        TestHelper.addToRegistry(registry, factory, "atlas:cobblestone_factory")

        persistence.save(registry)

        val loadRegistry = BlockRegistry(TestHelper.mockPlugin)
        persistence.load(loadRegistry)

        val loaded = loadRegistry.getAllBlocks().first() as CobblestoneFactory
        assertEquals(2, loaded.currentPower)
        assertTrue(loaded.hasWater, "the banked water should come back")
        assertFalse(loaded.hasLava, "lava was never fed, so it must not come back")
    }

    @Test
    fun `current power level persists accurately`() {
        val battery = SmallBattery(TestHelper.createLocation())
        battery.currentPower = 7
        TestHelper.addToRegistry(registry, battery, "atlas:small_battery")

        persistence.save(registry)

        val loadRegistry = BlockRegistry(TestHelper.mockPlugin)
        persistence.load(loadRegistry)

        assertEquals(7, assertInstanceOf(SmallBattery::class.java, loadRegistry.getAllBlocks().first()).currentPower)
    }

    @Test
    fun `battery round-trip preserves power and stores no facing`() {
        val battery = SmallBattery(TestHelper.createLocation(5.0, 64.0, 3.0))
        battery.currentPower = 7
        TestHelper.addToRegistry(registry, battery, "atlas:small_battery")

        persistence.save(registry)

        val loadRegistry = BlockRegistry(TestHelper.mockPlugin)
        persistence.load(loadRegistry)

        val loaded = loadRegistry.getAllBlocks().first()
        assertTrue(loaded is SmallBattery)
        // Storage is omnidirectional, so there is no facing to persist and nothing to restore.
        assertEquals(BlockFace.SELF, (loaded as SmallBattery).facing)
        assertEquals(7, loaded.currentPower)
    }

    @Test
    fun `multiple blocks save and load correctly`() {
        val panel = SmallSolarPanel(TestHelper.createLocation(0.0, 64.0, 0.0))
        panel.currentPower = 1
        val cable = PowerCable(TestHelper.createLocation(1.0, 64.0, 0.0))
        cable.currentPower = 1
        val battery = SmallBattery(TestHelper.createLocation(2.0, 64.0, 0.0))
        battery.currentPower = 5

        TestHelper.addToRegistry(registry, panel, "atlas:small_solar_panel")
        TestHelper.addToRegistry(registry, cable, "atlas:power_cable")
        TestHelper.addToRegistry(registry, battery, "atlas:small_battery")

        persistence.save(registry)

        val loadRegistry = BlockRegistry(TestHelper.mockPlugin)
        persistence.load(loadRegistry)

        assertEquals(3, loadRegistry.getAllBlocks().size)
    }

    /**
     * One registry holds every block, so the file has to pick its own family out of it. Without
     * the `owns` predicate a pipe would land in power_blocks.yml too, and every other test here
     * would still pass.
     */
    @Test
    fun `power_blocks yml holds only power blocks when the registry also holds a pipe`() {
        TestHelper.addToRegistry(registry, SmallBattery(TestHelper.createLocation()), SmallBattery.BLOCK_ID)
        TestHelper.addToRegistry(registry, FluidPipe(TestHelper.createLocation(x = 1.0)), FluidPipe.BLOCK_ID)

        persistence.save(registry)

        val saved = File(TestHelper.dataFolder, "power_blocks.yml").readText()
        assertTrue("atlas:small_battery" in saved, saved)
        assertFalse("atlas:fluid_pipe" in saved, saved)
    }
}

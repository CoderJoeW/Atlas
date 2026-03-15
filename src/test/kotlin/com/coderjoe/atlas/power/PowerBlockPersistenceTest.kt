package com.coderjoe.atlas.power

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.power.block.PowerCable
import com.coderjoe.atlas.power.block.SmallBattery
import com.coderjoe.atlas.power.block.SmallDrill
import com.coderjoe.atlas.power.block.SmallSolarPanel
import org.bukkit.block.BlockFace
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import java.io.File

class PowerBlockPersistenceTest {

    private lateinit var registry: PowerBlockRegistry
    private lateinit var persistence: PowerBlockPersistence

    @BeforeEach
    fun setup() {
        TestHelper.setup()
        registry = PowerBlockRegistry(TestHelper.mockPlugin)
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
        TestHelper.addToRegistry(registry, panel, "small_solar_panel")

        persistence.save(registry)

        // Create fresh registry for loading
        val loadRegistry = PowerBlockRegistry(TestHelper.mockPlugin)
        persistence.load(loadRegistry)

        val loaded = loadRegistry.getAllPowerBlocksWithIds()
        assertEquals(1, loaded.size)
        assertEquals("small_solar_panel", loaded[0].second)
        assertEquals(1, loaded[0].first.currentPower)
    }

    @Test
    fun `load from missing file does not error`() {
        val loadRegistry = PowerBlockRegistry(TestHelper.mockPlugin)
        assertDoesNotThrow { persistence.load(loadRegistry) }
        assertEquals(0, loadRegistry.getAllPowerBlocks().size)
    }

    @Test
    fun `drill enabled true persists correctly`() {
        val drill = SmallDrill(TestHelper.createLocation(), BlockFace.DOWN)
        drill.enabled = true
        drill.currentPower = 5
        TestHelper.addToRegistry(registry, drill, "small_drill_down")

        persistence.save(registry)

        val loadRegistry = PowerBlockRegistry(TestHelper.mockPlugin)
        persistence.load(loadRegistry)

        val loaded = loadRegistry.getAllPowerBlocks().first()
        assertTrue(loaded is SmallDrill)
        assertTrue((loaded as SmallDrill).enabled)
        assertEquals(5, loaded.currentPower)
    }

    @Test
    fun `drill enabled false persists correctly`() {
        val drill = SmallDrill(TestHelper.createLocation(), BlockFace.NORTH)
        drill.enabled = false
        TestHelper.addToRegistry(registry, drill, "small_drill_north")

        persistence.save(registry)

        val loadRegistry = PowerBlockRegistry(TestHelper.mockPlugin)
        persistence.load(loadRegistry)

        val loaded = loadRegistry.getAllPowerBlocks().first() as SmallDrill
        assertFalse(loaded.enabled)
    }

    @Test
    fun `facing direction persists for cables`() {
        val cable = PowerCable(TestHelper.createLocation(), BlockFace.EAST)
        TestHelper.addToRegistry(registry, cable, "power_cable_east")

        persistence.save(registry)

        val loadRegistry = PowerBlockRegistry(TestHelper.mockPlugin)
        persistence.load(loadRegistry)

        val loaded = loadRegistry.getAllPowerBlocks().first()
        assertTrue(loaded is PowerCable)
    }

    @Test
    fun `current power level persists accurately`() {
        val drill = SmallDrill(TestHelper.createLocation(), BlockFace.DOWN)
        drill.currentPower = 7
        TestHelper.addToRegistry(registry, drill, "small_drill_down")

        persistence.save(registry)

        val loadRegistry = PowerBlockRegistry(TestHelper.mockPlugin)
        persistence.load(loadRegistry)

        assertEquals(7, loadRegistry.getAllPowerBlocks().first().currentPower)
    }

    @Test
    fun `battery round-trip preserves facing and power`() {
        val battery = SmallBattery(TestHelper.createLocation(5.0, 64.0, 3.0), BlockFace.EAST)
        battery.currentPower = 7
        TestHelper.addToRegistry(registry, battery, "small_battery")

        persistence.save(registry)

        val loadRegistry = PowerBlockRegistry(TestHelper.mockPlugin)
        persistence.load(loadRegistry)

        val loaded = loadRegistry.getAllPowerBlocks().first()
        assertTrue(loaded is SmallBattery)
        assertEquals(BlockFace.EAST, (loaded as SmallBattery).facing)
        assertEquals(7, loaded.currentPower)
    }

    @Test
    fun `multiple blocks save and load correctly`() {
        val panel = SmallSolarPanel(TestHelper.createLocation(0.0, 64.0, 0.0))
        panel.currentPower = 1
        val cable = PowerCable(TestHelper.createLocation(1.0, 64.0, 0.0), BlockFace.NORTH)
        cable.currentPower = 1
        val drill = SmallDrill(TestHelper.createLocation(2.0, 64.0, 0.0), BlockFace.DOWN)
        drill.currentPower = 5

        TestHelper.addToRegistry(registry, panel, "small_solar_panel")
        TestHelper.addToRegistry(registry, cable, "power_cable_north")
        TestHelper.addToRegistry(registry, drill, "small_drill_down")

        persistence.save(registry)

        val loadRegistry = PowerBlockRegistry(TestHelper.mockPlugin)
        persistence.load(loadRegistry)

        assertEquals(3, loadRegistry.getAllPowerBlocks().size)
    }
}

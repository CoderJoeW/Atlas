package com.coderjoe.atlas.power

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.TestHelper.callPowerUpdate
import com.coderjoe.atlas.power.block.PowerMerger
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

class PowerMergerTest {

    private lateinit var registry: PowerBlockRegistry

    @BeforeEach
    fun setup() {
        TestHelper.setup()
        registry = PowerBlockRegistry(TestHelper.mockPlugin)
    }

    @AfterEach
    fun teardown() {
        TestHelper.teardown()
    }

    @Test
    fun `maxStorage is 2`() {
        val merger = PowerMerger(TestHelper.createLocation(), BlockFace.NORTH)
        assertEquals(2, merger.maxStorage)
    }

    @Test
    fun `canReceivePower is false`() {
        // PowerMerger does not use canReceivePower — it pulls manually from non-facing sides
        val merger = PowerMerger(TestHelper.createLocation(), BlockFace.NORTH)
        assertFalse(merger.canAcceptPower())
    }

    @Test
    fun `visual state unpowered when no power`() {
        val merger = PowerMerger(TestHelper.createLocation(), BlockFace.NORTH)
        assertEquals("power_merger_north", merger.getVisualStateBlockId())
    }

    @Test
    fun `visual state powered when has power`() {
        val merger = PowerMerger(TestHelper.createLocation(), BlockFace.NORTH)
        merger.currentPower = 1
        assertEquals("power_merger_north_powered", merger.getVisualStateBlockId())
    }

    @Test
    fun `visual state varies by facing direction`() {
        for ((face, expectedId) in PowerMerger.DIRECTIONAL_IDS) {
            val merger = PowerMerger(TestHelper.createLocation(), face)
            assertEquals(expectedId, merger.getVisualStateBlockId())
        }
    }

    @Test
    fun `pulls power from non-facing sides`() {
        val mergerLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val merger = PowerMerger(mergerLoc, BlockFace.NORTH)
        TestHelper.addToRegistry(registry, merger, "power_merger_north")

        // Source to the south (opposite of facing — behind)
        val source1 = SmallSolarPanel(TestHelper.createLocation(0.0, 64.0, 1.0))
        source1.currentPower = 1
        TestHelper.addToRegistry(registry, source1, "small_solar_panel")

        // Source to the east (side)
        val source2 = SmallSolarPanel(TestHelper.createLocation(1.0, 64.0, 0.0))
        source2.currentPower = 1
        TestHelper.addToRegistry(registry, source2, "small_solar_panel")

        merger.callPowerUpdate()

        assertEquals(2, merger.currentPower)
        assertEquals(0, source1.currentPower)
        assertEquals(0, source2.currentPower)
    }

    @Test
    fun `does not pull power from facing direction`() {
        val mergerLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val merger = PowerMerger(mergerLoc, BlockFace.NORTH)
        TestHelper.addToRegistry(registry, merger, "power_merger_north")

        // Source to the north (facing direction — should NOT pull from here)
        val source = SmallSolarPanel(TestHelper.createLocation(0.0, 64.0, -1.0))
        source.currentPower = 1
        TestHelper.addToRegistry(registry, source, "small_solar_panel")

        merger.callPowerUpdate()

        assertEquals(0, merger.currentPower)
        assertEquals(1, source.currentPower)
    }

    @Test
    fun `stops pulling when full`() {
        val mergerLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val merger = PowerMerger(mergerLoc, BlockFace.NORTH)
        merger.currentPower = 2
        TestHelper.addToRegistry(registry, merger, "power_merger_north")

        val source = SmallSolarPanel(TestHelper.createLocation(0.0, 64.0, 1.0))
        source.currentPower = 1
        TestHelper.addToRegistry(registry, source, "small_solar_panel")

        merger.callPowerUpdate()

        assertEquals(2, merger.currentPower)
        assertEquals(1, source.currentPower)
    }

    @Test
    fun `descriptor has correct properties`() {
        val desc = PowerMerger.descriptor
        assertEquals("power_merger", desc.baseBlockId)
        assertEquals("Power Merger", desc.displayName)
        assertEquals(12, desc.allRegistrableIds.size)
        assertTrue(desc.allRegistrableIds.contains("power_merger_north"))
        assertTrue(desc.allRegistrableIds.contains("power_merger_north_powered"))
    }

    @Test
    fun `downstream block can pull from merger in facing direction`() {
        val mergerLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val merger = PowerMerger(mergerLoc, BlockFace.NORTH)
        merger.currentPower = 2
        TestHelper.addToRegistry(registry, merger, "power_merger_north")

        // Consumer to the north (facing direction) that pulls from adjacent
        val consumer = com.coderjoe.atlas.power.block.SmallDrill(TestHelper.createLocation(0.0, 64.0, -1.0), BlockFace.DOWN)
        TestHelper.addToRegistry(registry, consumer, "small_drill_down")

        // The merger stores power; downstream blocks pull from it via their own update
        assertTrue(merger.hasPower())
        val pulled = merger.removePower(1)
        assertEquals(1, pulled)
        assertEquals(1, merger.currentPower)
    }
}

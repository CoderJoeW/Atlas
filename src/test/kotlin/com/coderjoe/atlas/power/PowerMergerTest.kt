package com.coderjoe.atlas.power

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.TestHelper.callPowerUpdate
import com.coderjoe.atlas.power.block.PowerMerger
import com.coderjoe.atlas.power.block.SmallSolarPanel
import org.bukkit.block.BlockFace
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
        val merger =
            PowerMerger(TestHelper.createLocation(), BlockFace.NORTH)
        assertEquals(2, merger.maxStorage)
    }

    @Test
    fun `canReceivePower is false`() {
        val merger =
            PowerMerger(TestHelper.createLocation(), BlockFace.NORTH)
        assertFalse(merger.canAcceptPower())
    }

    @Test
    fun `visual state always returns BLOCK_ID`() {
        val merger =
            PowerMerger(TestHelper.createLocation(), BlockFace.NORTH)
        assertEquals(
            "atlas:power_merger",
            merger.getVisualStateBlockId(),
        )
        merger.currentPower = 1
        assertEquals(
            "atlas:power_merger",
            merger.getVisualStateBlockId(),
        )
    }

    @Test
    fun `pulls power from non-facing sides`() {
        val mergerLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val merger = PowerMerger(mergerLoc, BlockFace.NORTH)
        TestHelper.addToRegistry(
            registry,
            merger,
            "atlas:power_merger",
        )

        val source1 =
            SmallSolarPanel(TestHelper.createLocation(0.0, 64.0, 1.0))
        source1.currentPower = 1
        TestHelper.addToRegistry(
            registry,
            source1,
            "atlas:small_solar_panel",
        )

        val source2 =
            SmallSolarPanel(TestHelper.createLocation(1.0, 64.0, 0.0))
        source2.currentPower = 1
        TestHelper.addToRegistry(
            registry,
            source2,
            "atlas:small_solar_panel",
        )

        merger.callPowerUpdate()

        assertEquals(2, merger.currentPower)
        assertEquals(0, source1.currentPower)
        assertEquals(0, source2.currentPower)
    }

    @Test
    fun `does not pull power from facing direction`() {
        val mergerLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val merger = PowerMerger(mergerLoc, BlockFace.NORTH)
        TestHelper.addToRegistry(
            registry,
            merger,
            "atlas:power_merger",
        )

        val source =
            SmallSolarPanel(
                TestHelper.createLocation(0.0, 64.0, -1.0),
            )
        source.currentPower = 1
        TestHelper.addToRegistry(
            registry,
            source,
            "atlas:small_solar_panel",
        )

        merger.callPowerUpdate()

        assertEquals(0, merger.currentPower)
        assertEquals(1, source.currentPower)
    }

    @Test
    fun `stops pulling when full`() {
        val mergerLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val merger = PowerMerger(mergerLoc, BlockFace.NORTH)
        merger.currentPower = 2
        TestHelper.addToRegistry(
            registry,
            merger,
            "atlas:power_merger",
        )

        val source =
            SmallSolarPanel(TestHelper.createLocation(0.0, 64.0, 1.0))
        source.currentPower = 1
        TestHelper.addToRegistry(
            registry,
            source,
            "atlas:small_solar_panel",
        )

        merger.callPowerUpdate()

        assertEquals(2, merger.currentPower)
        assertEquals(1, source.currentPower)
    }

    @Test
    fun `descriptor has correct properties`() {
        val desc = PowerMerger.descriptor
        assertEquals("atlas:power_merger", desc.baseBlockId)
        assertEquals("Power Merger", desc.displayName)
    }

    @Test
    fun `downstream block can pull from merger in facing direction`() {
        val mergerLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val merger = PowerMerger(mergerLoc, BlockFace.NORTH)
        merger.currentPower = 2
        TestHelper.addToRegistry(
            registry,
            merger,
            "atlas:power_merger",
        )

        val consumer =
            com.coderjoe.atlas.utility.block.SmallDrill(
                TestHelper.createLocation(0.0, 64.0, -1.0),
                BlockFace.DOWN,
            )
        TestHelper.addToRegistry(
            registry,
            consumer,
            "atlas:small_drill",
        )

        assertTrue(merger.hasPower())
        val pulled = merger.removePower(1)
        assertEquals(1, pulled)
        assertEquals(1, merger.currentPower)
    }
}

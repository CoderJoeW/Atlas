package com.coderjoe.atlas.power

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.TestHelper.callPowerUpdate
import com.coderjoe.atlas.power.block.PowerCable
import com.coderjoe.atlas.power.block.SmallBattery
import com.coderjoe.atlas.power.block.SmallSolarPanel
import io.mockk.every
import org.bukkit.block.BlockFace
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SmallSolarPanelTest {
    private companion object {
        const val NIGHT_TIME = 18000L
    }

    @BeforeEach
    fun setup() {
        TestHelper.setup()
    }

    @AfterEach
    fun teardown() {
        TestHelper.teardown()
    }

    @Test
    fun `small solar panel maxStorage is 4`() {
        val panel = SmallSolarPanel(TestHelper.createLocation())
        assertEquals(4, panel.maxStorage)
    }

    @Test
    fun `small solar panel canReceivePower is false`() {
        val panel = SmallSolarPanel(TestHelper.createLocation())
        assertFalse(panel.canAcceptPower())
    }

    @Test
    fun `small solar panel visual state is dark at night`() {
        every { TestHelper.mockWorld.time } returns NIGHT_TIME
        val panel = SmallSolarPanel(TestHelper.createLocation())
        assertEquals("atlas:small_solar_panel", panel.getVisualStateBlockId())
    }

    @Test
    fun `small solar panel visual state is lit while collecting sunlight`() {
        every { TestHelper.mockWorld.time } returns 6000L
        val panel = SmallSolarPanel(TestHelper.createLocation())
        assertEquals("atlas:small_solar_panel_active", panel.getVisualStateBlockId())
    }

    @Test
    fun `small solar panel visual state ignores how much charge is buffered`() {
        val panel = SmallSolarPanel(TestHelper.createLocation())

        // The readout answers "is it working", so every charge level looks the same within a
        // given time of day - only daylight moves it.
        for (power in 0..panel.maxStorage) {
            panel.currentPower = power

            every { TestHelper.mockWorld.time } returns 6000L
            assertEquals("atlas:small_solar_panel_active", panel.getVisualStateBlockId(), "day, $power power")

            every { TestHelper.mockWorld.time } returns NIGHT_TIME
            assertEquals("atlas:small_solar_panel", panel.getVisualStateBlockId(), "night, $power power")
        }
    }

    @Test
    fun `small solar panel generates power during daytime`() {
        val panel = SmallSolarPanel(TestHelper.createLocation())
        panel.callPowerUpdate()
        assertEquals(2, panel.currentPower)
    }

    @Test
    fun `small solar panel does not generate power at night`() {
        every { TestHelper.mockWorld.time } returns NIGHT_TIME

        val panel = SmallSolarPanel(TestHelper.createLocation())
        panel.callPowerUpdate()

        assertEquals(0, panel.currentPower)
    }

    @Test
    fun `small solar panel does not exceed max storage`() {
        val panel = SmallSolarPanel(TestHelper.createLocation())
        repeat(5) { panel.callPowerUpdate() }
        assertEquals(4, panel.currentPower)
    }

    @Test
    fun `small solar panel outputs through its base pad only`() {
        val panel = SmallSolarPanel(TestHelper.createLocation())

        assertTrue(panel.canOutputToward(BlockFace.DOWN))
        for (face in listOf(BlockFace.UP, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST)) {
            assertFalse(panel.canOutputToward(face), "panel should not output toward $face")
        }
    }

    @Test
    fun `small solar panel refuses extraction from a sealed face`() {
        val panel = SmallSolarPanel(TestHelper.createLocation())
        panel.currentPower = 4

        assertEquals(0, panel.removePowerToward(BlockFace.NORTH, 1))
        assertEquals(4, panel.currentPower)
    }

    @Test
    fun `small solar panel allows extraction through its output face`() {
        val panel = SmallSolarPanel(TestHelper.createLocation())
        panel.currentPower = 4

        assertEquals(1, panel.removePowerToward(BlockFace.DOWN, 1))
        assertEquals(3, panel.currentPower)
    }

    @Test
    fun `small solar panel pushes stored power into the block below`() {
        val registry = PowerBlockRegistry(TestHelper.mockPlugin)
        every { TestHelper.mockWorld.time } returns 6000L

        val panel = SmallSolarPanel(TestHelper.createLocation(0.0, 64.0, 0.0))
        val battery = SmallBattery(TestHelper.createLocation(0.0, 63.0, 0.0))

        TestHelper.addToRegistry(registry, panel, "atlas:small_solar_panel")
        TestHelper.addToRegistry(registry, battery, "atlas:small_battery")

        panel.callPowerUpdate()

        assertEquals(2, battery.currentPower)
        assertEquals(0, panel.currentPower)
    }

    @Test
    fun `small solar panel does not push into a cable beside it`() {
        val registry = PowerBlockRegistry(TestHelper.mockPlugin)
        every { TestHelper.mockWorld.time } returns 6000L

        val panel = SmallSolarPanel(TestHelper.createLocation(0.0, 64.0, 0.0))
        // Cable to the SOUTH facing SOUTH, so it would pull from the panel behind it
        val cable = PowerCable(TestHelper.createLocation(0.0, 64.0, 1.0))

        TestHelper.addToRegistry(registry, panel, "atlas:small_solar_panel")
        TestHelper.addToRegistry(registry, cable, "atlas:power_cable")

        panel.callPowerUpdate()
        cable.callPowerUpdate()

        assertEquals(0, cable.currentPower)
        assertEquals(2, panel.currentPower)
    }

    @Test
    fun `small solar panel holds power when nothing sits below it`() {
        val registry = PowerBlockRegistry(TestHelper.mockPlugin)
        every { TestHelper.mockWorld.time } returns 6000L

        val panel = SmallSolarPanel(TestHelper.createLocation(0.0, 64.0, 0.0))
        TestHelper.addToRegistry(registry, panel, "atlas:small_solar_panel")

        panel.callPowerUpdate()
        panel.callPowerUpdate()

        assertEquals(4, panel.currentPower)
    }

    @Test
    fun `small solar panel descriptor registers the active variant`() {
        val descriptor = SmallSolarPanel.descriptor

        assertEquals("atlas:small_solar_panel", descriptor.baseBlockId)
        assertEquals("Small Solar Panel", descriptor.displayName)
        assertEquals(listOf("atlas:small_solar_panel_active"), descriptor.additionalBlockIds)
    }

    @Test
    fun `small solar panel does not push into a cable facing the wrong way`() {
        val registry = PowerBlockRegistry(TestHelper.mockPlugin)
        every { TestHelper.mockWorld.time } returns 6000L

        val panel = SmallSolarPanel(TestHelper.createLocation(0.0, 64.0, 0.0))
        // a cable directly below, but pulling from the east rather than from above
        val cable = PowerCable(TestHelper.createLocation(0.0, 63.0, 0.0))

        TestHelper.addToRegistry(registry, panel, "atlas:small_solar_panel")
        TestHelper.addToRegistry(registry, cable, "atlas:power_cable")

        panel.callPowerUpdate()

        // the cable's only input is its east face, so it must not be filled from above,
        // which would otherwise strand power it can never discharge
        assertEquals(0, cable.currentPower)
        assertEquals(2, panel.currentPower)
    }

    @Test
    fun `small solar panel keeps its power when the push is refused`() {
        val registry = PowerBlockRegistry(TestHelper.mockPlugin)
        every { TestHelper.mockWorld.time } returns 6000L

        val panel = SmallSolarPanel(TestHelper.createLocation(0.0, 64.0, 0.0))
        panel.currentPower = 3
        val cable = PowerCable(TestHelper.createLocation(0.0, 63.0, 0.0))
        TestHelper.addToRegistry(registry, panel, "atlas:small_solar_panel")
        TestHelper.addToRegistry(registry, cable, "atlas:power_cable")

        panel.callPowerUpdate()

        // refused push must be refunded, never lost or duplicated
        assertEquals(0, cable.currentPower)
        assertEquals(4, panel.currentPower)
    }
}

package com.coderjoe.atlas.block.power

import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.testing.Blocks.placedIn
import com.coderjoe.atlas.testing.MockServer
import io.mockk.every
import org.bukkit.block.BlockFace
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SmallSolarPanelTest {
    private lateinit var registry: BlockRegistry

    private companion object {
        const val NIGHT_TIME = 18000L
    }

    @BeforeEach
    fun setup() {
        MockServer.setup()
        registry = BlockRegistry(MockServer.plugin)
    }

    @AfterEach
    fun teardown() {
        MockServer.teardown()
    }

    /**
     * Generation is gated behind [SmallSolarPanel.GENERATION_INTERVAL_TICKS] of accumulated ticks
     * so the panel's own tick rate can stay fast (for a responsive day/night appearance) without
     * generating every tick. Tests that want a single `powerUpdate()` call to generate need to
     * fast-forward the counter first, exactly like setting `currentPower` directly elsewhere.
     */
    private fun SmallSolarPanel.forceGenerationDue() {
        ticksSinceGeneration = SmallSolarPanel.GENERATION_INTERVAL_TICKS
    }

    @Test
    fun `small solar panel maxStorage is 4`() {
        val panel = SmallSolarPanel(MockServer.createLocation())
        assertEquals(4, panel.maxStorage)
    }

    @Test
    fun `small solar panel canReceivePower is false`() {
        val panel = SmallSolarPanel(MockServer.createLocation())
        assertFalse(panel.canAcceptPower())
    }

    @Test
    fun `small solar panel visual state is dark at night`() {
        every { MockServer.world.time } returns NIGHT_TIME
        val panel = SmallSolarPanel(MockServer.createLocation())
        assertEquals("atlas:small_solar_panel", panel.getVisualStateBlockId())
    }

    @Test
    fun `small solar panel visual state is lit while collecting sunlight`() {
        every { MockServer.world.time } returns 6000L
        val panel = SmallSolarPanel(MockServer.createLocation())
        assertEquals("atlas:small_solar_panel_active", panel.getVisualStateBlockId())
    }

    @Test
    fun `small solar panel visual state ignores how much charge is buffered`() {
        val panel = SmallSolarPanel(MockServer.createLocation())

        // The readout answers "is it working", so every charge level looks the same within a
        // given time of day - only daylight moves it.
        for (power in 0..panel.maxStorage) {
            panel.currentPower = power

            every { MockServer.world.time } returns 6000L
            assertEquals("atlas:small_solar_panel_active", panel.getVisualStateBlockId(), "day, $power power")

            every { MockServer.world.time } returns NIGHT_TIME
            assertEquals("atlas:small_solar_panel", panel.getVisualStateBlockId(), "night, $power power")
        }
    }

    @Test
    fun `small solar panel generates power during daytime`() {
        val panel = SmallSolarPanel(MockServer.createLocation()).placedIn(registry)
        panel.forceGenerationDue()
        panel.powerUpdate()
        assertEquals(1, panel.currentPower)
    }

    @Test
    fun `small solar panel does not generate power at night`() {
        every { MockServer.world.time } returns NIGHT_TIME

        val panel = SmallSolarPanel(MockServer.createLocation())
        panel.forceGenerationDue()
        panel.powerUpdate()

        assertEquals(0, panel.currentPower)
    }

    @Test
    fun `small solar panel does not exceed max storage`() {
        val panel = SmallSolarPanel(MockServer.createLocation()).placedIn(registry)
        repeat(5) {
            panel.forceGenerationDue()
            panel.powerUpdate()
        }
        assertEquals(4, panel.currentPower)
    }

    @Test
    fun `small solar panel outputs through its base pad only`() {
        val panel = SmallSolarPanel(MockServer.createLocation())

        assertTrue(panel.canOutputToward(BlockFace.DOWN))
        for (face in listOf(BlockFace.UP, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST)) {
            assertFalse(panel.canOutputToward(face), "panel should not output toward $face")
        }
    }

    @Test
    fun `small solar panel refuses extraction from a sealed face`() {
        val panel = SmallSolarPanel(MockServer.createLocation())
        panel.currentPower = 4

        assertEquals(0, panel.removePowerToward(BlockFace.NORTH, 1))
        assertEquals(4, panel.currentPower)
    }

    @Test
    fun `small solar panel allows extraction through its output face`() {
        val panel = SmallSolarPanel(MockServer.createLocation())
        panel.currentPower = 4

        assertEquals(1, panel.removePowerToward(BlockFace.DOWN, 1))
        assertEquals(3, panel.currentPower)
    }

    @Test
    fun `small solar panel pushes stored power into the block below`() {
        every { MockServer.world.time } returns 6000L

        val panel = SmallSolarPanel(MockServer.createLocation(0.0, 64.0, 0.0))
        val battery = SmallBattery(MockServer.createLocation(0.0, 63.0, 0.0))

        registry.track(panel, "atlas:small_solar_panel")
        registry.track(battery, "atlas:small_battery")

        panel.forceGenerationDue()
        panel.powerUpdate()

        assertEquals(1, battery.currentPower)
        assertEquals(0, panel.currentPower)
    }

    @Test
    fun `small solar panel does not push into a cable beside it`() {
        every { MockServer.world.time } returns 6000L

        val panel = SmallSolarPanel(MockServer.createLocation(0.0, 64.0, 0.0))
        // Cable to the SOUTH facing SOUTH, so it would pull from the panel behind it
        val cable = PowerCable(MockServer.createLocation(0.0, 64.0, 1.0))

        registry.track(panel, "atlas:small_solar_panel")
        registry.track(cable, "atlas:power_cable")

        panel.forceGenerationDue()
        panel.powerUpdate()
        cable.powerUpdate()

        assertEquals(0, cable.currentPower)
        assertEquals(1, panel.currentPower)
    }

    @Test
    fun `small solar panel holds power when nothing sits below it`() {
        every { MockServer.world.time } returns 6000L

        val panel = SmallSolarPanel(MockServer.createLocation(0.0, 64.0, 0.0))
        registry.track(panel, "atlas:small_solar_panel")

        panel.forceGenerationDue()
        panel.powerUpdate()
        panel.forceGenerationDue()
        panel.powerUpdate()

        assertEquals(2, panel.currentPower)
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
        every { MockServer.world.time } returns 6000L

        val panel = SmallSolarPanel(MockServer.createLocation(0.0, 64.0, 0.0))
        // a cable directly below, but pulling from the east rather than from above
        val cable = PowerCable(MockServer.createLocation(0.0, 63.0, 0.0))

        registry.track(panel, "atlas:small_solar_panel")
        registry.track(cable, "atlas:power_cable")

        panel.forceGenerationDue()
        panel.powerUpdate()

        // the cable's only input is its east face, so it must not be filled from above,
        // which would otherwise strand power it can never discharge
        assertEquals(0, cable.currentPower)
        assertEquals(1, panel.currentPower)
    }

    @Test
    fun `small solar panel keeps its power when the push is refused`() {
        every { MockServer.world.time } returns 6000L

        val panel = SmallSolarPanel(MockServer.createLocation(0.0, 64.0, 0.0))
        panel.currentPower = 3
        val cable = PowerCable(MockServer.createLocation(0.0, 63.0, 0.0))
        registry.track(panel, "atlas:small_solar_panel")
        registry.track(cable, "atlas:power_cable")

        panel.forceGenerationDue()
        panel.powerUpdate()

        // refused push must be refunded, never lost or duplicated
        assertEquals(0, cable.currentPower)
        assertEquals(4, panel.currentPower)
    }
}

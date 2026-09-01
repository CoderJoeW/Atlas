package com.coderjoe.atlas.power

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.TestHelper.callPowerUpdate
import com.coderjoe.atlas.TestHelper.callSpawnEffects
import com.coderjoe.atlas.power.block.SmallSolarPanel
import io.mockk.every
import io.mockk.verify
import org.bukkit.Particle
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SmallSolarPanelTest {
    private companion object {
        const val NIGHT_TIME = 18000L
        const val PANEL_X = 0.5
        const val PANEL_Z = 0.5
        val PANEL_Y = 64.0 + 0.7
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
    fun `small solar panel visual state idle when no power`() {
        val panel = SmallSolarPanel(TestHelper.createLocation())
        assertEquals("atlas:small_solar_panel", panel.getVisualStateBlockId())
    }

    @Test
    fun `small solar panel visual state full when charged`() {
        val panel = SmallSolarPanel(TestHelper.createLocation())
        panel.currentPower = 2
        assertEquals("atlas:small_solar_panel_full", panel.getVisualStateBlockId())
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
    fun `small solar panel emits particles above the panel during daytime`() {
        val panel = SmallSolarPanel(TestHelper.createLocation())

        panel.callSpawnEffects()

        verify(exactly = 1) {
            TestHelper.mockWorld.spawnParticle(
                Particle.ELECTRIC_SPARK,
                PANEL_X, PANEL_Y, PANEL_Z,
                2, 0.3, 0.05, 0.3, 0.0,
            )
        }
    }

    @Test
    fun `small solar panel does not emit particles at night`() {
        every { TestHelper.mockWorld.time } returns NIGHT_TIME

        val panel = SmallSolarPanel(TestHelper.createLocation())
        panel.callSpawnEffects()

        verify(exactly = 0) {
            TestHelper.mockWorld.spawnParticle(
                Particle.ELECTRIC_SPARK,
                PANEL_X, PANEL_Y, PANEL_Z,
                2, 0.3, 0.05, 0.3, 0.0,
            )
        }
    }

    @Test
    fun `small solar panel descriptor registers the full variant`() {
        val descriptor = SmallSolarPanel.descriptor

        assertEquals("atlas:small_solar_panel", descriptor.baseBlockId)
        assertEquals("Small Solar Panel", descriptor.displayName)
        assertEquals(listOf("atlas:small_solar_panel_full"), descriptor.additionalBlockIds)
    }
}

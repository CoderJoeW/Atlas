package com.coderjoe.atlas.power

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.power.block.PowerCable
import com.coderjoe.atlas.power.block.SmallBattery
import com.coderjoe.atlas.power.block.SmallSolarPanel
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PowerNetworkReportTest {
    private lateinit var registry: PowerBlockRegistry
    private lateinit var player: Player
    private lateinit var messages: MutableList<Component>

    @BeforeEach
    fun setup() {
        TestHelper.setup()
        registry = PowerBlockRegistry(TestHelper.mockPlugin)
        player = mockk(relaxed = true)
        messages = mutableListOf()
        val captured = slot<Component>()
        every { player.sendMessage(capture(captured)) } answers { messages.add(captured.captured) }
    }

    @AfterEach
    fun teardown() {
        TestHelper.teardown()
    }

    /** Every line the readout produced, flattened so assertions can look for wording. */
    private fun transcript(): String = messages.joinToString("\n") { it.toString() }

    @Test
    fun `a block with no cable reports as standalone`() {
        val battery = SmallBattery(TestHelper.createLocation(0.0, 64.0, 0.0))
        battery.currentPower = 7
        TestHelper.addToRegistry(registry, battery, "atlas:small_battery")

        PowerNetworkReport.report(player, battery)

        assertTrue(transcript().contains("Not joined to any cable"))
        assertTrue(transcript().contains("7"))
    }

    @Test
    fun `a run with a producer and a consumer reports as flowing`() {
        val cable = PowerCable(TestHelper.createLocation(0.0, 64.0, 0.0))
        TestHelper.addToRegistry(registry, cable, "atlas:power_cable")

        val panel = SmallSolarPanel(TestHelper.createLocation(0.0, 65.0, 0.0))
        panel.currentPower = 3
        TestHelper.addToRegistry(registry, panel, "atlas:small_solar_panel")

        val battery = SmallBattery(TestHelper.createLocation(0.0, 63.0, 0.0))
        TestHelper.addToRegistry(registry, battery, "atlas:small_battery")

        PowerNetworkReport.report(player, cable)

        assertTrue(transcript().contains("Power is flowing"))
    }

    @Test
    fun `a run with no generator says so`() {
        val cable = PowerCable(TestHelper.createLocation(0.0, 64.0, 0.0))
        TestHelper.addToRegistry(registry, cable, "atlas:power_cable")

        val battery = SmallBattery(TestHelper.createLocation(0.0, 63.0, 0.0))
        TestHelper.addToRegistry(registry, battery, "atlas:small_battery")

        PowerNetworkReport.report(player, cable)

        assertTrue(transcript().contains("No generator is feeding this run"))
    }

    @Test
    fun `a run with nothing that can take power says so`() {
        val cable = PowerCable(TestHelper.createLocation(0.0, 64.0, 0.0))
        TestHelper.addToRegistry(registry, cable, "atlas:power_cable")

        val panel = SmallSolarPanel(TestHelper.createLocation(0.0, 65.0, 0.0))
        panel.currentPower = 3
        TestHelper.addToRegistry(registry, panel, "atlas:small_solar_panel")

        PowerNetworkReport.report(player, cable)

        assertTrue(transcript().contains("Nothing on this run can take power"))
    }

    @Test
    fun `an empty run says nothing is attached`() {
        val cable = PowerCable(TestHelper.createLocation(0.0, 64.0, 0.0))
        TestHelper.addToRegistry(registry, cable, "atlas:power_cable")

        PowerNetworkReport.report(player, cable)

        assertTrue(transcript().contains("Nothing is attached to this run yet"))
    }

    @Test
    fun `reporting on a block beside the run finds that run`() {
        val cable = PowerCable(TestHelper.createLocation(0.0, 64.0, 0.0))
        TestHelper.addToRegistry(registry, cable, "atlas:power_cable")
        val second = PowerCable(TestHelper.createLocation(0.0, 63.0, 0.0))
        TestHelper.addToRegistry(registry, second, "atlas:power_cable")

        val panel = SmallSolarPanel(TestHelper.createLocation(0.0, 65.0, 0.0))
        panel.currentPower = 1
        TestHelper.addToRegistry(registry, panel, "atlas:small_solar_panel")

        // asked about the panel, not the cable, and still reports the run it feeds
        PowerNetworkReport.report(player, panel)

        assertTrue(transcript().contains("Power Network"))
        assertTrue(transcript().contains("2 blocks"))
        verify(atLeast = 1) { player.sendMessage(any<Component>()) }
        assertEquals(6, messages.size)
    }
}

package com.coderjoe.atlas.scenario

import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.block.power.LavaGenerator
import com.coderjoe.atlas.block.power.PowerCable
import com.coderjoe.atlas.block.power.PowerNetworks
import com.coderjoe.atlas.block.power.SmallBattery
import com.coderjoe.atlas.block.power.SmallSolarPanel
import com.coderjoe.atlas.testing.MockServer
import io.mockk.every
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PowerNetworkIntegrationTest {
    private lateinit var registry: BlockRegistry

    @BeforeEach
    fun setup() {
        MockServer.setup()
        registry = BlockRegistry(MockServer.plugin)
    }

    @AfterEach
    fun teardown() {
        MockServer.teardown()
    }

    @Test
    fun `a cable run carries power from the panel to a battery at the far end`() {
        every { MockServer.world.time } returns 6000L

        // panel on top, three cables dropping away from its base pad, battery at the bottom
        val solar = SmallSolarPanel(MockServer.createLocation(0.0, 64.0, 0.0))
        val cables =
            (61..63).map { y ->
                PowerCable(MockServer.createLocation(0.0, y.toDouble(), 0.0))
            }
        val battery = SmallBattery(MockServer.createLocation(0.0, 60.0, 0.0))

        registry.track(solar, "atlas:small_solar_panel")
        cables.forEach { registry.track(it, "atlas:power_cable") }
        registry.track(battery, "atlas:small_battery")

        solar.ticksSinceGeneration = SmallSolarPanel.GENERATION_INTERVAL_TICKS
        solar.powerUpdate()
        assertEquals(1, solar.currentPower)

        // one network tick moves the charge the whole length of the run, not one block per tick.
        // Every cable ticks in game; only the run's leader actually performs the transfer.
        cables.forEach { it.powerUpdate() }

        assertEquals(0, solar.currentPower)
        assertEquals(1, battery.currentPower)
        assertTrue(cables.all { it.currentPower == 0 })
    }

    @Test
    fun `every cable in a run reports the same network`() {
        val cables =
            (60..64).map { y ->
                PowerCable(MockServer.createLocation(0.0, y.toDouble(), 0.0))
            }
        cables.forEach { registry.track(it, "atlas:power_cable") }

        val networks = cables.map { PowerNetworks.networkFor(it) }
        for (network in networks) {
            assertEquals(5, network.cables.size)
        }
        // and they all agree on which one of them runs the transfer
        val leaders = networks.map { it.leader }.distinct()
        assertEquals(1, leaders.size)
    }

    @Test
    fun `a break in the run splits it into two networks`() {
        val far = PowerCable(MockServer.createLocation(0.0, 64.0, 0.0))
        val near = PowerCable(MockServer.createLocation(0.0, 63.0, 0.0))
        val gapped = PowerCable(MockServer.createLocation(0.0, 61.0, 0.0))

        for (cable in listOf(far, near, gapped)) {
            registry.track(cable, "atlas:power_cable")
        }

        assertEquals(2, PowerNetworks.networkFor(far).cables.size)
        assertEquals(1, PowerNetworks.networkFor(gapped).cables.size)
    }

    @Test
    fun `a branch in the run feeds two consumers at once`() {
        every { MockServer.world.time } returns 6000L

        val solar = SmallSolarPanel(MockServer.createLocation(0.0, 64.0, 0.0))
        val junction = PowerCable(MockServer.createLocation(0.0, 63.0, 0.0))
        val eastArm = PowerCable(MockServer.createLocation(1.0, 63.0, 0.0))
        val westArm = PowerCable(MockServer.createLocation(-1.0, 63.0, 0.0))

        registry.track(solar, "atlas:small_solar_panel")
        for (cable in listOf(junction, eastArm, westArm)) {
            registry.track(cable, "atlas:power_cable")
        }

        val east = SmallBattery(MockServer.createLocation(2.0, 63.0, 0.0))
        val west = SmallBattery(MockServer.createLocation(-2.0, 63.0, 0.0))
        registry.track(east, "atlas:small_battery")
        registry.track(west, "atlas:small_battery")

        // Two network ticks: the panel now generates 1 power/tick, so it takes two ticks to
        // supply one unit to each branch.
        repeat(2) {
            solar.ticksSinceGeneration = SmallSolarPanel.GENERATION_INTERVAL_TICKS
            solar.powerUpdate()
            for (cable in listOf(junction, eastArm, westArm)) cable.powerUpdate()
        }

        // the run splits without any splitter block: a unit to each branch
        assertEquals(1, east.currentPower)
        assertEquals(1, west.currentPower)
        assertEquals(0, solar.currentPower)
    }

    @Test
    fun `battery accumulates power over ticks`() {
        every { MockServer.world.time } returns 6000L

        val solar = SmallSolarPanel(MockServer.createLocation(0.0, 64.0, 0.0))
        // Battery sits under the panel's base pad and takes its output
        val battery = SmallBattery(MockServer.createLocation(0.0, 63.0, 0.0))

        registry.track(solar, "atlas:small_solar_panel")
        registry.track(battery, "atlas:small_battery")

        // Tick 1: solar generates 1 and pushes all of it into the battery
        solar.ticksSinceGeneration = SmallSolarPanel.GENERATION_INTERVAL_TICKS
        solar.powerUpdate()
        battery.powerUpdate()
        assertEquals(1, battery.currentPower)

        // Tick 2: solar generates again and pushes again
        solar.ticksSinceGeneration = SmallSolarPanel.GENERATION_INTERVAL_TICKS
        solar.powerUpdate()
        battery.powerUpdate()
        assertEquals(2, battery.currentPower)
    }

    @Test
    fun `cable only pulls from behind, not sides`() {
        val cable = PowerCable(MockServer.createLocation(0.0, 64.0, 0.0))

        // Source to the EAST (side, not behind)
        val source = LavaGenerator(MockServer.createLocation(1.0, 64.0, 0.0))
        source.currentPower = 1

        registry.track(cable, "atlas:power_cable")
        registry.track(source, "atlas:lava_generator")

        cable.powerUpdate()
        assertEquals(0, cable.currentPower) // did not pull
        assertEquals(1, source.currentPower) // unchanged
    }

    @Test
    fun `full chain - solar to cable to cable to battery`() {
        every { MockServer.world.time } returns 6000L

        val solar = SmallSolarPanel(MockServer.createLocation(0.0, 64.0, 0.0))
        val cable1 = PowerCable(MockServer.createLocation(0.0, 63.0, 0.0))
        val cable2 = PowerCable(MockServer.createLocation(0.0, 62.0, 0.0))
        val battery = SmallBattery(MockServer.createLocation(0.0, 61.0, 0.0))

        registry.track(solar, "atlas:small_solar_panel")
        registry.track(cable1, "atlas:power_cable")
        registry.track(cable2, "atlas:power_cable")
        registry.track(battery, "atlas:small_battery")

        // Simulate several ticks of power flowing through the chain
        repeat(3) {
            solar.ticksSinceGeneration = SmallSolarPanel.GENERATION_INTERVAL_TICKS
            solar.powerUpdate()
            cable1.powerUpdate()
            cable2.powerUpdate()
            battery.powerUpdate()
        }

        // Battery should have accumulated power over the ticks
        assertTrue(battery.currentPower > 0, "Battery should have accumulated some power")
    }
}

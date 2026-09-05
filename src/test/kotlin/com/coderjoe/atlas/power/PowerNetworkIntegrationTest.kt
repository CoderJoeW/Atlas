package com.coderjoe.atlas.power

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.TestHelper.callPowerUpdate
import com.coderjoe.atlas.power.block.LavaGenerator
import com.coderjoe.atlas.power.block.PowerCable
import com.coderjoe.atlas.power.block.SmallBattery
import com.coderjoe.atlas.power.block.SmallSolarPanel
import io.mockk.every
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PowerNetworkIntegrationTest {
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
    fun `a cable run carries power from the panel to a battery at the far end`() {
        every { TestHelper.mockWorld.time } returns 6000L

        // panel on top, three cables dropping away from its base pad, battery at the bottom
        val solar = SmallSolarPanel(TestHelper.createLocation(0.0, 64.0, 0.0))
        val cables =
            (61..63).map { y ->
                PowerCable(TestHelper.createLocation(0.0, y.toDouble(), 0.0))
            }
        val battery = SmallBattery(TestHelper.createLocation(0.0, 60.0, 0.0))

        TestHelper.addToRegistry(registry, solar, "atlas:small_solar_panel")
        cables.forEach { TestHelper.addToRegistry(registry, it, "atlas:power_cable") }
        TestHelper.addToRegistry(registry, battery, "atlas:small_battery")

        solar.callPowerUpdate()
        assertEquals(2, solar.currentPower)

        // one network tick moves the charge the whole length of the run, not one block per tick.
        // Every cable ticks in game; only the run's leader actually performs the transfer.
        cables.forEach { it.callPowerUpdate() }

        assertEquals(0, solar.currentPower)
        assertEquals(2, battery.currentPower)
        assertTrue(cables.all { it.currentPower == 0 })
    }

    @Test
    fun `every cable in a run reports the same network`() {
        val cables =
            (60..64).map { y ->
                PowerCable(TestHelper.createLocation(0.0, y.toDouble(), 0.0))
            }
        cables.forEach { TestHelper.addToRegistry(registry, it, "atlas:power_cable") }

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
        val far = PowerCable(TestHelper.createLocation(0.0, 64.0, 0.0))
        val near = PowerCable(TestHelper.createLocation(0.0, 63.0, 0.0))
        val gapped = PowerCable(TestHelper.createLocation(0.0, 61.0, 0.0))

        for (cable in listOf(far, near, gapped)) {
            TestHelper.addToRegistry(registry, cable, "atlas:power_cable")
        }

        assertEquals(2, PowerNetworks.networkFor(far).cables.size)
        assertEquals(1, PowerNetworks.networkFor(gapped).cables.size)
    }

    @Test
    fun `a branch in the run feeds two consumers at once`() {
        every { TestHelper.mockWorld.time } returns 6000L

        val solar = SmallSolarPanel(TestHelper.createLocation(0.0, 64.0, 0.0))
        val junction = PowerCable(TestHelper.createLocation(0.0, 63.0, 0.0))
        val eastArm = PowerCable(TestHelper.createLocation(1.0, 63.0, 0.0))
        val westArm = PowerCable(TestHelper.createLocation(-1.0, 63.0, 0.0))

        TestHelper.addToRegistry(registry, solar, "atlas:small_solar_panel")
        for (cable in listOf(junction, eastArm, westArm)) {
            TestHelper.addToRegistry(registry, cable, "atlas:power_cable")
        }

        val east = SmallBattery(TestHelper.createLocation(2.0, 63.0, 0.0))
        val west = SmallBattery(TestHelper.createLocation(-2.0, 63.0, 0.0))
        TestHelper.addToRegistry(registry, east, "atlas:small_battery")
        TestHelper.addToRegistry(registry, west, "atlas:small_battery")

        solar.callPowerUpdate()
        for (cable in listOf(junction, eastArm, westArm)) cable.callPowerUpdate()

        // the run splits without any splitter block: a unit to each branch
        assertEquals(1, east.currentPower)
        assertEquals(1, west.currentPower)
        assertEquals(0, solar.currentPower)
    }

    @Test
    fun `battery accumulates power over ticks`() {
        every { TestHelper.mockWorld.time } returns 6000L

        val solar = SmallSolarPanel(TestHelper.createLocation(0.0, 64.0, 0.0))
        // Battery sits under the panel's base pad and takes its output
        val battery = SmallBattery(TestHelper.createLocation(0.0, 63.0, 0.0))

        TestHelper.addToRegistry(registry, solar, "atlas:small_solar_panel")
        TestHelper.addToRegistry(registry, battery, "atlas:small_battery")

        // Tick 1: solar generates 2 and pushes all of it into the battery
        solar.callPowerUpdate()
        battery.callPowerUpdate()
        assertEquals(2, battery.currentPower)

        // Tick 2: solar generates again and pushes again
        solar.callPowerUpdate()
        battery.callPowerUpdate()
        assertEquals(4, battery.currentPower)
    }

    @Test
    fun `cable only pulls from behind, not sides`() {
        val cable = PowerCable(TestHelper.createLocation(0.0, 64.0, 0.0))

        // Source to the EAST (side, not behind)
        val source = LavaGenerator(TestHelper.createLocation(1.0, 64.0, 0.0))
        source.currentPower = 1

        TestHelper.addToRegistry(registry, cable, "atlas:power_cable")
        TestHelper.addToRegistry(registry, source, "atlas:lava_generator")

        cable.callPowerUpdate()
        assertEquals(0, cable.currentPower) // did not pull
        assertEquals(1, source.currentPower) // unchanged
    }

    @Test
    fun `full chain - solar to cable to cable to battery`() {
        every { TestHelper.mockWorld.time } returns 6000L

        val solar = SmallSolarPanel(TestHelper.createLocation(0.0, 64.0, 0.0))
        val cable1 = PowerCable(TestHelper.createLocation(0.0, 63.0, 0.0))
        val cable2 = PowerCable(TestHelper.createLocation(0.0, 62.0, 0.0))
        val battery = SmallBattery(TestHelper.createLocation(0.0, 61.0, 0.0))

        TestHelper.addToRegistry(registry, solar, "atlas:small_solar_panel")
        TestHelper.addToRegistry(registry, cable1, "atlas:power_cable")
        TestHelper.addToRegistry(registry, cable2, "atlas:power_cable")
        TestHelper.addToRegistry(registry, battery, "atlas:small_battery")

        // Simulate several ticks of power flowing through the chain
        repeat(3) {
            solar.callPowerUpdate()
            cable1.callPowerUpdate()
            cable2.callPowerUpdate()
            battery.callPowerUpdate()
        }

        // Battery should have accumulated power over the ticks
        assertTrue(battery.currentPower > 0, "Battery should have accumulated some power")
    }
}

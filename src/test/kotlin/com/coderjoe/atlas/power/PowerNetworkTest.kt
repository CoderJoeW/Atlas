package com.coderjoe.atlas.power

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.TestHelper.callPowerUpdate
import com.coderjoe.atlas.power.block.PowerCable
import com.coderjoe.atlas.power.block.SmallBattery
import com.coderjoe.atlas.power.block.SmallSolarPanel
import org.bukkit.block.BlockFace
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PowerNetworkTest {
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

    private fun cable(
        x: Double,
        y: Double,
        z: Double,
    ): PowerCable =
        PowerCable(TestHelper.createLocation(x, y, z)).also {
            TestHelper.addToRegistry(registry, it, "atlas:power_cable")
        }

    @Test
    fun `a lone cable is its own network`() {
        val only = cable(0.0, 64.0, 0.0)
        val network = PowerNetworks.networkFor(only)
        assertEquals(1, network.cables.size)
        assertTrue(network.leader === only)
    }

    @Test
    fun `discovery follows cable around corners`() {
        val a = cable(0.0, 64.0, 0.0)
        cable(0.0, 64.0, 1.0)
        cable(1.0, 64.0, 1.0)
        cable(1.0, 65.0, 1.0)

        assertEquals(4, PowerNetworks.networkFor(a).cables.size)
    }

    @Test
    fun `a cable joins a solar panel only from below its output face`() {
        val panel = SmallSolarPanel(TestHelper.createLocation(0.0, 64.0, 0.0))
        TestHelper.addToRegistry(registry, panel, "atlas:small_solar_panel")

        val beneath = cable(0.0, 63.0, 0.0)
        val beside = cable(0.0, 64.0, 1.0)

        // the panel hands power out of its base, so only the cable under it gets an arm
        assertTrue(BlockFace.UP in beneath.connections(), "cable below should join the panel")
        assertFalse(BlockFace.NORTH in beside.connections(), "cable beside should not join the panel")
    }

    @Test
    fun `a cable joins a battery on every side`() {
        val battery = SmallBattery(TestHelper.createLocation(0.0, 64.0, 0.0))
        TestHelper.addToRegistry(registry, battery, "atlas:small_battery")

        assertTrue(BlockFace.UP in cable(0.0, 63.0, 0.0).connections())
        assertTrue(BlockFace.DOWN in cable(0.0, 65.0, 0.0).connections())
        assertTrue(BlockFace.NORTH in cable(0.0, 64.0, 1.0).connections())
    }

    @Test
    fun `a full battery still shows its connection`() {
        val battery = SmallBattery(TestHelper.createLocation(0.0, 64.0, 0.0))
        battery.currentPower = battery.maxStorage
        TestHelper.addToRegistry(registry, battery, "atlas:small_battery")

        // connections describe the port, not the moment - a battery that fills up must not
        // appear to unplug itself
        assertTrue(BlockFace.UP in cable(0.0, 63.0, 0.0).connections())
    }

    @Test
    fun `a block touching the run is sorted into producers and consumers`() {
        val run = cable(0.0, 64.0, 0.0)

        val panel = SmallSolarPanel(TestHelper.createLocation(0.0, 65.0, 0.0))
        panel.currentPower = 3
        TestHelper.addToRegistry(registry, panel, "atlas:small_solar_panel")

        val battery = SmallBattery(TestHelper.createLocation(0.0, 63.0, 0.0))
        TestHelper.addToRegistry(registry, battery, "atlas:small_battery")

        val (sources, sinks) = PowerNetworks.networkFor(run).terminals()

        assertEquals(1, sources.size)
        assertTrue(sources.single().block === panel)
        // the panel only lets power out of its base, so the face pointing back at the cable is DOWN
        assertEquals(BlockFace.DOWN, sources.single().faceTowardCable)
        assertEquals(1, sinks.size)
        assertTrue(sinks.single().block === battery)
    }

    @Test
    fun `a panel that cannot output toward the cable is not a producer`() {
        val run = cable(0.0, 64.0, 0.0)

        // panel beside the run: its only output face is its base, which points at nothing
        val panel = SmallSolarPanel(TestHelper.createLocation(1.0, 64.0, 0.0))
        panel.currentPower = 3
        TestHelper.addToRegistry(registry, panel, "atlas:small_solar_panel")

        val (sources, _) = PowerNetworks.networkFor(run).terminals()
        assertTrue(sources.isEmpty())
    }

    @Test
    fun `transfer shares power evenly between consumers`() {
        val run = cable(0.0, 64.0, 0.0)

        val panel = SmallSolarPanel(TestHelper.createLocation(0.0, 65.0, 0.0))
        panel.currentPower = 4
        TestHelper.addToRegistry(registry, panel, "atlas:small_solar_panel")

        val north = SmallBattery(TestHelper.createLocation(0.0, 64.0, -1.0))
        val south = SmallBattery(TestHelper.createLocation(0.0, 64.0, 1.0))
        for (battery in listOf(north, south)) {
            TestHelper.addToRegistry(registry, battery, "atlas:small_battery")
        }

        val moved = PowerNetworks.networkFor(run).transfer()

        assertEquals(4, moved)
        assertEquals(0, panel.currentPower)
        assertEquals(2, north.currentPower)
        assertEquals(2, south.currentPower)
    }

    @Test
    fun `transfer does nothing without a producer`() {
        val run = cable(0.0, 64.0, 0.0)
        val battery = SmallBattery(TestHelper.createLocation(0.0, 63.0, 0.0))
        TestHelper.addToRegistry(registry, battery, "atlas:small_battery")

        assertEquals(0, PowerNetworks.networkFor(run).transfer())
        assertEquals(0, battery.currentPower)
    }

    @Test
    fun `a battery on the run is never made to feed itself`() {
        val run = cable(0.0, 64.0, 0.0)

        val battery = SmallBattery(TestHelper.createLocation(0.0, 63.0, 0.0))
        battery.currentPower = 5
        TestHelper.addToRegistry(registry, battery, "atlas:small_battery")

        // it is the only block on the run, so it is both the sole producer and the sole consumer
        assertEquals(0, PowerNetworks.networkFor(run).transfer())
        assertEquals(5, battery.currentPower)
    }

    @Test
    fun `a run lights up from a generator alone, before anything draws from it`() {
        val run = cable(0.0, 64.0, 0.0)

        // a panel sitting on the cable with no consumer anywhere on the run
        val panel = SmallSolarPanel(TestHelper.createLocation(0.0, 65.0, 0.0))
        panel.currentPower = 2
        TestHelper.addToRegistry(registry, panel, "atlas:small_solar_panel")

        val network = PowerNetworks.networkFor(run)
        assertEquals(0, network.transfer(), "nothing to draw the power, so none moves")
        assertTrue(network.hasSupply(), "but the run is still fed and must read as live")

        run.callPowerUpdate()
        assertTrue(run.carrying, "the cable should render lit, not dead")
    }

    @Test
    fun `a run with no generator stays dark`() {
        val run = cable(0.0, 64.0, 0.0)
        val battery = SmallBattery(TestHelper.createLocation(0.0, 63.0, 0.0))
        TestHelper.addToRegistry(registry, battery, "atlas:small_battery")

        assertFalse(PowerNetworks.networkFor(run).hasSupply())
        run.callPowerUpdate()
        assertFalse(run.carrying)
    }

    @Test
    fun `a cable answers for its run when asked to supply power`() {
        val run = cable(0.0, 64.0, 0.0)
        assertFalse(run.canSupplyPower())

        val panel = SmallSolarPanel(TestHelper.createLocation(0.0, 65.0, 0.0))
        panel.currentPower = 2
        TestHelper.addToRegistry(registry, panel, "atlas:small_solar_panel")

        assertTrue(run.canSupplyPower())
        // and a consumer drawing on the cable really draws off the producer
        assertEquals(1, run.removePowerToward(BlockFace.NORTH, 1))
        assertEquals(1, panel.currentPower)
    }
}

package com.coderjoe.atlas.power

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.TestHelper.callPowerUpdate
import com.coderjoe.atlas.power.block.PowerCable
import com.coderjoe.atlas.power.block.SmallBattery
import com.coderjoe.atlas.power.block.SmallSolarPanel
import com.coderjoe.atlas.utility.block.SmallDrill
import io.mockk.every
import io.mockk.mockk
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
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
    fun `solar generates power and cable pulls it`() {
        every { TestHelper.mockWorld.time } returns 6000L

        // Solar at (0,64,0)
        val solar = SmallSolarPanel(TestHelper.createLocation(0.0, 64.0, 0.0))
        // Cable at (0,64,1) facing SOUTH (pulls from NORTH = behind = z-1 = solar)
        val cable = PowerCable(TestHelper.createLocation(0.0, 64.0, 1.0), BlockFace.SOUTH)

        TestHelper.addToRegistry(registry, solar, "atlas:small_solar_panel")
        TestHelper.addToRegistry(registry, cable, "atlas:power_cable")

        // Solar generates
        solar.callPowerUpdate()
        assertEquals(1, solar.currentPower)

        // Cable pulls from solar
        cable.callPowerUpdate()
        assertEquals(1, cable.currentPower)
        assertEquals(0, solar.currentPower)
    }

    @Test
    fun `chain propagation - solar to cable to cable`() {
        every { TestHelper.mockWorld.time } returns 6000L

        val solar = SmallSolarPanel(TestHelper.createLocation(0.0, 64.0, 0.0))
        val cable1 = PowerCable(TestHelper.createLocation(0.0, 64.0, 1.0), BlockFace.SOUTH)
        val cable2 = PowerCable(TestHelper.createLocation(0.0, 64.0, 2.0), BlockFace.SOUTH)

        TestHelper.addToRegistry(registry, solar, "atlas:small_solar_panel")
        TestHelper.addToRegistry(registry, cable1, "atlas:power_cable")
        TestHelper.addToRegistry(registry, cable2, "atlas:power_cable")

        // Tick 1: solar generates
        solar.callPowerUpdate()
        assertEquals(1, solar.currentPower)

        // Tick 1: cable1 pulls from solar
        cable1.callPowerUpdate()
        assertEquals(1, cable1.currentPower)

        // Tick 1: cable2 pulls from cable1
        cable2.callPowerUpdate()
        assertEquals(1, cable2.currentPower)
        assertEquals(0, cable1.currentPower)
    }

    @Test
    fun `battery accumulates power over ticks`() {
        every { TestHelper.mockWorld.time } returns 6000L

        val solar = SmallSolarPanel(TestHelper.createLocation(0.0, 64.0, 0.0))
        // Battery facing SOUTH, pulls from behind (NORTH = z-1 = solar)
        val battery = SmallBattery(TestHelper.createLocation(0.0, 64.0, 1.0), BlockFace.SOUTH)

        TestHelper.addToRegistry(registry, solar, "atlas:small_solar_panel")
        TestHelper.addToRegistry(registry, battery, "atlas:small_battery")

        // Tick 1: solar generates, battery pulls
        solar.callPowerUpdate()
        battery.callPowerUpdate()
        assertEquals(1, battery.currentPower)

        // Tick 2: solar generates again, battery pulls again
        solar.callPowerUpdate()
        battery.callPowerUpdate()
        assertEquals(2, battery.currentPower)
    }

    @Test
    fun `cable only pulls from behind, not sides`() {
        val cable = PowerCable(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.NORTH)

        // Source to the EAST (side, not behind)
        val source = SmallSolarPanel(TestHelper.createLocation(1.0, 64.0, 0.0))
        source.currentPower = 1

        TestHelper.addToRegistry(registry, cable, "atlas:power_cable")
        TestHelper.addToRegistry(registry, source, "atlas:small_solar_panel")

        cable.callPowerUpdate()
        assertEquals(0, cable.currentPower) // did not pull
        assertEquals(1, source.currentPower) // unchanged
    }

    @Test
    fun `drill pulls from all adjacent neighbors`() {
        val drill = SmallDrill(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.DOWN)
        drill.currentPower = 0

        // Place powered sources in multiple directions
        val source1 = SmallSolarPanel(TestHelper.createLocation(1.0, 64.0, 0.0))
        source1.currentPower = 1
        val source2 = SmallSolarPanel(TestHelper.createLocation(0.0, 65.0, 0.0))
        source2.currentPower = 1
        val source3 = SmallSolarPanel(TestHelper.createLocation(0.0, 64.0, 1.0))
        source3.currentPower = 1

        TestHelper.addToRegistry(registry, drill, "atlas:small_drill")
        TestHelper.addToRegistry(registry, source1, "atlas:small_solar_panel")
        TestHelper.addToRegistry(registry, source2, "atlas:small_solar_panel")
        TestHelper.addToRegistry(registry, source3, "atlas:small_solar_panel")

        // Mock blocks below so mining scan doesn't error
        for (y in 63 downTo -64) {
            val block = mockk<Block>(relaxed = true)
            every { block.type } returns Material.AIR
            every { TestHelper.mockWorld.getBlockAt(0, y, 0) } returns block
        }

        drill.callPowerUpdate()
        assertEquals(3, drill.currentPower) // pulled from all 3
    }

    @Test
    fun `full chain - solar to cable to cable to battery to drill`() {
        every { TestHelper.mockWorld.time } returns 6000L

        val solar = SmallSolarPanel(TestHelper.createLocation(0.0, 64.0, 0.0))
        val cable1 = PowerCable(TestHelper.createLocation(0.0, 64.0, 1.0), BlockFace.SOUTH)
        val cable2 = PowerCable(TestHelper.createLocation(0.0, 64.0, 2.0), BlockFace.SOUTH)
        val battery = SmallBattery(TestHelper.createLocation(0.0, 64.0, 3.0), BlockFace.SOUTH)

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

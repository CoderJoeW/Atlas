package com.coderjoe.atlas.power

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.TestHelper.callPowerUpdate
import com.coderjoe.atlas.power.block.LavaGenerator
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

/**
 * The battery is passive, omnidirectional storage: it is filled and drained by whatever it
 * touches and never reaches out on its own, so it has no front to line up when placed.
 */
class SmallBatteryTest {
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

    private fun battery(
        x: Double = 0.0,
        y: Double = 64.0,
        z: Double = 0.0,
    ): SmallBattery =
        SmallBattery(TestHelper.createLocation(x, y, z)).also {
            TestHelper.addToRegistry(registry, it, "atlas:small_battery")
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
    fun `battery holds 50 power`() {
        assertEquals(50, battery().maxStorage)
    }

    @Test
    fun `battery is storage`() {
        assertTrue(battery().isStorage)
    }

    @Test
    fun `battery has no facing`() {
        assertEquals(BlockFace.SELF, battery().facing)
    }

    @Test
    fun `battery accepts power until it is full`() {
        val battery = battery()
        assertTrue(battery.canAcceptPower())

        battery.currentPower = battery.maxStorage
        assertFalse(battery.canAcceptPower())
    }

    @Test
    fun `battery takes power pushed in through any face`() {
        for (face in com.coderjoe.atlas.core.AtlasBlock.ADJACENT_FACES) {
            val battery = SmallBattery(TestHelper.createLocation())
            assertEquals(1, battery.addPowerFrom(face, 1), "should accept from $face")
        }
    }

    @Test
    fun `battery gives power out through any face`() {
        for (face in com.coderjoe.atlas.core.AtlasBlock.ADJACENT_FACES) {
            val battery = SmallBattery(TestHelper.createLocation())
            battery.currentPower = 1
            assertEquals(1, battery.removePowerToward(face, 1), "should give toward $face")
        }
    }

    @Test
    fun `battery visual state steps through every charge level`() {
        val battery = battery()

        val expected =
            listOf(
                0 to "atlas:small_battery",
                1 to "atlas:small_battery_low",
                12 to "atlas:small_battery_low",
                13 to "atlas:small_battery_medium",
                25 to "atlas:small_battery_medium",
                26 to "atlas:small_battery_high",
                37 to "atlas:small_battery_high",
                38 to "atlas:small_battery_full",
                50 to "atlas:small_battery_full",
            )

        for ((power, state) in expected) {
            battery.currentPower = power
            assertEquals(state, battery.getVisualStateBlockId(), "charge $power")
        }
    }

    @Test
    fun `battery leaves a neighbouring generator alone`() {
        val battery = battery()

        val generator = LavaGenerator(TestHelper.createLocation(0.0, 64.0, 1.0))
        generator.currentPower = 5
        TestHelper.addToRegistry(registry, generator, "atlas:lava_generator")

        battery.callPowerUpdate()

        assertEquals(0, battery.currentPower)
        assertEquals(5, generator.currentPower)
    }

    @Test
    fun `battery does not push into a neighbour on its own`() {
        val battery = battery()
        battery.currentPower = 10

        val other = SmallBattery(TestHelper.createLocation(0.0, 64.0, 1.0))
        TestHelper.addToRegistry(registry, other, "atlas:small_battery")

        battery.callPowerUpdate()

        assertEquals(10, battery.currentPower)
        assertEquals(0, other.currentPower)
    }

    @Test
    fun `a cable run fills a battery from a panel`() {
        val panel = SmallSolarPanel(TestHelper.createLocation(0.0, 65.0, 0.0))
        panel.currentPower = 3
        TestHelper.addToRegistry(registry, panel, "atlas:small_solar_panel")

        val run = cable(0.0, 64.0, 0.0)
        val battery = battery(0.0, 63.0, 0.0)

        run.callPowerUpdate()

        assertEquals(3, battery.currentPower)
        assertEquals(0, panel.currentPower)
    }

    @Test
    fun `two batteries on one run level themselves out`() {
        val run = cable(0.0, 64.0, 0.0)

        val charged = battery(0.0, 65.0, 0.0)
        charged.currentPower = 20
        val empty = battery(0.0, 63.0, 0.0)

        run.callPowerUpdate()

        assertEquals(10, charged.currentPower)
        assertEquals(10, empty.currentPower)
    }

    @Test
    fun `batteries already level are left alone`() {
        val run = cable(0.0, 64.0, 0.0)

        val a = battery(0.0, 65.0, 0.0)
        a.currentPower = 10
        val b = battery(0.0, 63.0, 0.0)
        b.currentPower = 10

        run.callPowerUpdate()

        assertEquals(10, a.currentPower)
        assertEquals(10, b.currentPower)
    }

    @Test
    fun `batteries one unit apart do not swap places forever`() {
        val run = cable(0.0, 64.0, 0.0)

        val ahead = battery(0.0, 65.0, 0.0)
        ahead.currentPower = 11
        val behind = battery(0.0, 63.0, 0.0)
        behind.currentPower = 10

        // A single unit cannot close a gap of one - it only swaps which battery leads - so the
        // pair has to be left as it is, or it would oscillate for as long as the run existed.
        repeat(3) { run.callPowerUpdate() }

        assertEquals(11, ahead.currentPower)
        assertEquals(10, behind.currentPower)
    }

    @Test
    fun `an odd total settles within a single unit and stays put`() {
        val run = cable(0.0, 64.0, 0.0)

        val charged = battery(0.0, 65.0, 0.0)
        charged.currentPower = 7
        val empty = battery(0.0, 63.0, 0.0)

        repeat(3) { run.callPowerUpdate() }

        assertEquals(7, charged.currentPower + empty.currentPower, "no charge may be created or lost")
        assertTrue(
            kotlin.math.abs(charged.currentPower - empty.currentPower) <= 1,
            "bank should end level to within one unit, got ${charged.currentPower} and ${empty.currentPower}",
        )
    }

    @Test
    fun `a battery bank of three levels out`() {
        val run = cable(0.0, 64.0, 0.0)
        cable(1.0, 64.0, 0.0)

        val full = battery(0.0, 65.0, 0.0)
        full.currentPower = 30
        val mid = battery(0.0, 63.0, 0.0)
        mid.currentPower = 6
        val empty = battery(1.0, 65.0, 0.0)

        repeat(5) { run.callPowerUpdate() }

        val totals = listOf(full.currentPower, mid.currentPower, empty.currentPower)
        assertEquals(36, totals.sum(), "no charge may be created or lost")
        assertTrue(
            totals.max() - totals.min() <= 1,
            "bank should end level to within one unit, got $totals",
        )
    }
}

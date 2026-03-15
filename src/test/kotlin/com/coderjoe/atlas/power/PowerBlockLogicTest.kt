package com.coderjoe.atlas.power

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.TestHelper.callPowerUpdate
import com.coderjoe.atlas.power.block.PowerCable
import com.coderjoe.atlas.power.block.SmallBattery
import com.coderjoe.atlas.power.block.SmallSolarPanel
import com.coderjoe.atlas.utility.block.SmallDrill
import io.mockk.every
import org.bukkit.block.BlockFace
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PowerBlockLogicTest {
    @BeforeEach
    fun setup() {
        TestHelper.setup()
    }

    @AfterEach
    fun teardown() {
        TestHelper.teardown()
    }

    // --- PowerBlock base class (via SmallBattery, maxStorage=10) ---

    @Test
    fun `addPower on empty block returns amount added`() {
        val block =
            SmallBattery(TestHelper.createLocation(), BlockFace.NORTH)
        val added = block.addPower(5)
        assertEquals(5, added)
        assertEquals(5, block.currentPower)
    }

    @Test
    fun `addPower caps at maxStorage`() {
        val block =
            SmallBattery(TestHelper.createLocation(), BlockFace.NORTH)
        val added = block.addPower(15)
        assertEquals(10, added)
        assertEquals(10, block.currentPower)
    }

    @Test
    fun `addPower with partial space returns space available`() {
        val block =
            SmallBattery(TestHelper.createLocation(), BlockFace.NORTH)
        block.currentPower = 8
        val added = block.addPower(3)
        assertEquals(2, added)
        assertEquals(10, block.currentPower)
    }

    @Test
    fun `removePower returns amount removed`() {
        val block =
            SmallBattery(TestHelper.createLocation(), BlockFace.NORTH)
        block.currentPower = 5
        val removed = block.removePower(3)
        assertEquals(3, removed)
        assertEquals(2, block.currentPower)
    }

    @Test
    fun `removePower caps at currentPower`() {
        val block =
            SmallBattery(TestHelper.createLocation(), BlockFace.NORTH)
        block.currentPower = 3
        val removed = block.removePower(10)
        assertEquals(3, removed)
        assertEquals(0, block.currentPower)
    }

    @Test
    fun `removePower on empty block returns 0`() {
        val block =
            SmallBattery(TestHelper.createLocation(), BlockFace.NORTH)
        val removed = block.removePower(1)
        assertEquals(0, removed)
        assertEquals(0, block.currentPower)
    }

    @Test
    fun `hasPower returns true when power greater than 0`() {
        val block =
            SmallBattery(TestHelper.createLocation(), BlockFace.NORTH)
        block.currentPower = 1
        assertTrue(block.hasPower())
    }

    @Test
    fun `hasPower returns false when power is 0`() {
        val block =
            SmallBattery(TestHelper.createLocation(), BlockFace.NORTH)
        assertFalse(block.hasPower())
    }

    @Test
    fun `canAcceptPower returns true when below max and canReceivePower`() {
        val block =
            SmallBattery(TestHelper.createLocation(), BlockFace.NORTH)
        assertTrue(block.canAcceptPower())
    }

    @Test
    fun `canAcceptPower returns false when full`() {
        val block =
            SmallBattery(TestHelper.createLocation(), BlockFace.NORTH)
        block.currentPower = 10
        assertFalse(block.canAcceptPower())
    }

    @Test
    fun `canAcceptPower returns false when canReceivePower is false`() {
        val block = SmallSolarPanel(TestHelper.createLocation())
        assertFalse(block.canAcceptPower())
    }

    // --- SmallSolarPanel specifics ---

    @Test
    fun `solar panel canReceivePower is false`() {
        val panel = SmallSolarPanel(TestHelper.createLocation())
        assertFalse(panel.canAcceptPower())
    }

    @Test
    fun `solar panel maxStorage is 1`() {
        val panel = SmallSolarPanel(TestHelper.createLocation())
        assertEquals(1, panel.maxStorage)
    }

    @Test
    fun `solar panel visual state empty when no power`() {
        val panel = SmallSolarPanel(TestHelper.createLocation())
        assertEquals(
            "atlas:small_solar_panel",
            panel.getVisualStateBlockId(),
        )
    }

    @Test
    fun `solar panel visual state full when has power`() {
        val panel = SmallSolarPanel(TestHelper.createLocation())
        panel.currentPower = 1
        assertEquals(
            "atlas:small_solar_panel_full",
            panel.getVisualStateBlockId(),
        )
    }

    @Test
    fun `solar panel generates power during daytime`() {
        every { TestHelper.mockWorld.time } returns 6000L
        val panel = SmallSolarPanel(TestHelper.createLocation())
        panel.callPowerUpdate()
        assertEquals(1, panel.currentPower)
    }

    @Test
    fun `solar panel does not generate power at night`() {
        every { TestHelper.mockWorld.time } returns 13000L
        val panel = SmallSolarPanel(TestHelper.createLocation())
        panel.callPowerUpdate()
        assertEquals(0, panel.currentPower)
    }

    @Test
    fun `solar panel does not overflow past maxStorage`() {
        every { TestHelper.mockWorld.time } returns 6000L
        val panel = SmallSolarPanel(TestHelper.createLocation())
        panel.currentPower = 1
        panel.callPowerUpdate()
        assertEquals(1, panel.currentPower)
    }

    // --- SmallBattery specifics ---

    @Test
    fun `battery maxStorage is 10`() {
        val battery =
            SmallBattery(TestHelper.createLocation(), BlockFace.NORTH)
        assertEquals(10, battery.maxStorage)
    }

    @Test
    fun `battery facing defaults to DOWN when SELF`() {
        val battery =
            SmallBattery(TestHelper.createLocation(), BlockFace.SELF)
        assertEquals(BlockFace.DOWN, battery.facing)
    }

    @Test
    fun `battery visual state empty when power 0`() {
        val battery =
            SmallBattery(TestHelper.createLocation(), BlockFace.NORTH)
        assertEquals(
            "atlas:small_battery",
            battery.getVisualStateBlockId(),
        )
    }

    @Test
    fun `battery visual state low when power 1-3`() {
        val battery =
            SmallBattery(TestHelper.createLocation(), BlockFace.NORTH)
        for (p in 1..3) {
            battery.currentPower = p
            assertEquals(
                "atlas:small_battery_low",
                battery.getVisualStateBlockId(),
                "Failed for power=$p",
            )
        }
    }

    @Test
    fun `battery visual state medium when power 4-7`() {
        val battery =
            SmallBattery(TestHelper.createLocation(), BlockFace.NORTH)
        for (p in 4..7) {
            battery.currentPower = p
            assertEquals(
                "atlas:small_battery_medium",
                battery.getVisualStateBlockId(),
                "Failed for power=$p",
            )
        }
    }

    @Test
    fun `battery visual state full when power 8-10`() {
        val battery =
            SmallBattery(TestHelper.createLocation(), BlockFace.NORTH)
        for (p in 8..10) {
            battery.currentPower = p
            assertEquals(
                "atlas:small_battery_full",
                battery.getVisualStateBlockId(),
                "Failed for power=$p",
            )
        }
    }

    @Test
    fun `battery pulls power from block behind it`() {
        val registry = PowerBlockRegistry(TestHelper.mockPlugin)
        val batteryLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val battery = SmallBattery(batteryLoc, BlockFace.NORTH)

        val sourceLoc = TestHelper.createLocation(0.0, 64.0, 1.0)
        val source = SmallSolarPanel(sourceLoc)
        source.currentPower = 1

        TestHelper.addToRegistry(
            registry,
            battery,
            "atlas:small_battery",
        )
        TestHelper.addToRegistry(
            registry,
            source,
            "atlas:small_solar_panel",
        )

        battery.callPowerUpdate()
        assertEquals(1, battery.currentPower)
        assertEquals(0, source.currentPower)
    }

    @Test
    fun `battery does not pull when already full`() {
        val registry = PowerBlockRegistry(TestHelper.mockPlugin)
        val battery =
            SmallBattery(TestHelper.createLocation(), BlockFace.NORTH)
        battery.currentPower = 10

        battery.callPowerUpdate()
        assertEquals(10, battery.currentPower)
    }

    // --- PowerCable specifics ---

    @Test
    fun `cable maxStorage is 1`() {
        val cable =
            PowerCable(TestHelper.createLocation(), BlockFace.NORTH)
        assertEquals(1, cable.maxStorage)
    }

    @Test
    fun `cable visual state always returns BLOCK_ID`() {
        val cable =
            PowerCable(TestHelper.createLocation(), BlockFace.NORTH)
        assertEquals(
            "atlas:power_cable",
            cable.getVisualStateBlockId(),
        )
        cable.currentPower = 1
        assertEquals(
            "atlas:power_cable",
            cable.getVisualStateBlockId(),
        )
    }

    @Test
    fun `cable pulls from source behind it`() {
        val registry = PowerBlockRegistry(TestHelper.mockPlugin)
        val cableLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val cable = PowerCable(cableLoc, BlockFace.NORTH)

        val sourceLoc = TestHelper.createLocation(0.0, 64.0, 1.0)
        val source = SmallSolarPanel(sourceLoc)
        source.currentPower = 1

        TestHelper.addToRegistry(
            registry,
            cable,
            "atlas:power_cable",
        )
        TestHelper.addToRegistry(
            registry,
            source,
            "atlas:small_solar_panel",
        )

        cable.callPowerUpdate()
        assertEquals(1, cable.currentPower)
        assertEquals(0, source.currentPower)
    }

    @Test
    fun `cable does not pull from blocks in other directions`() {
        val registry = PowerBlockRegistry(TestHelper.mockPlugin)
        val cableLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val cable = PowerCable(cableLoc, BlockFace.NORTH)

        val sourceLoc = TestHelper.createLocation(1.0, 64.0, 0.0)
        val source = SmallSolarPanel(sourceLoc)
        source.currentPower = 1

        TestHelper.addToRegistry(
            registry,
            cable,
            "atlas:power_cable",
        )
        TestHelper.addToRegistry(
            registry,
            source,
            "atlas:small_solar_panel",
        )

        cable.callPowerUpdate()
        assertEquals(0, cable.currentPower)
        assertEquals(1, source.currentPower)
    }

    // --- SmallDrill specifics ---

    @Test
    fun `drill maxStorage is 10`() {
        val drill = SmallDrill(TestHelper.createLocation())
        assertEquals(10, drill.maxStorage)
    }

    @Test
    fun `drill toggleEnabled flips state`() {
        val drill = SmallDrill(TestHelper.createLocation())
        assertTrue(drill.enabled)
        drill.toggleEnabled()
        assertFalse(drill.enabled)
        drill.toggleEnabled()
        assertTrue(drill.enabled)
    }

    @Test
    fun `drill miningDirection defaults to DOWN when null`() {
        val drill = SmallDrill(TestHelper.createLocation(), null)
        assertEquals(BlockFace.DOWN, drill.miningDirection)
    }

    @Test
    fun `drill miningDirection defaults to DOWN when SELF`() {
        val drill =
            SmallDrill(TestHelper.createLocation(), BlockFace.SELF)
        assertEquals(BlockFace.DOWN, drill.miningDirection)
    }

    @Test
    fun `drill visual state always returns BLOCK_ID`() {
        val drill =
            SmallDrill(TestHelper.createLocation(), BlockFace.NORTH)
        assertEquals(
            "atlas:small_drill",
            drill.getVisualStateBlockId(),
        )
    }

    @Test
    fun `drill disabled does nothing on powerUpdate`() {
        val registry = PowerBlockRegistry(TestHelper.mockPlugin)
        val drill =
            SmallDrill(TestHelper.createLocation(), BlockFace.DOWN)
        drill.enabled = false
        drill.currentPower = 10

        drill.callPowerUpdate()
        assertEquals(10, drill.currentPower)
    }

    // --- Solar panel time boundary tests ---

    @Test
    fun `solar panel generates power at time 0`() {
        every { TestHelper.mockWorld.time } returns 0L
        val panel = SmallSolarPanel(TestHelper.createLocation())
        panel.callPowerUpdate()
        assertEquals(1, panel.currentPower)
    }

    @Test
    fun `solar panel generates power at time 12000`() {
        every { TestHelper.mockWorld.time } returns 12000L
        val panel = SmallSolarPanel(TestHelper.createLocation())
        panel.callPowerUpdate()
        assertEquals(1, panel.currentPower)
    }

    @Test
    fun `solar panel does not generate power at time 12001`() {
        every { TestHelper.mockWorld.time } returns 12001L
        val panel = SmallSolarPanel(TestHelper.createLocation())
        panel.callPowerUpdate()
        assertEquals(0, panel.currentPower)
    }

    // --- Battery facing preservation ---

    @Test
    fun `battery facing preserves NORTH`() {
        val battery =
            SmallBattery(TestHelper.createLocation(), BlockFace.NORTH)
        assertEquals(BlockFace.NORTH, battery.facing)
    }

    @Test
    fun `battery facing preserves EAST`() {
        val battery =
            SmallBattery(TestHelper.createLocation(), BlockFace.EAST)
        assertEquals(BlockFace.EAST, battery.facing)
    }

    @Test
    fun `battery facing preserves UP`() {
        val battery =
            SmallBattery(TestHelper.createLocation(), BlockFace.UP)
        assertEquals(BlockFace.UP, battery.facing)
    }

    // --- Battery powerUpdate edge cases ---

    @Test
    fun `battery powerUpdate when source has no power`() {
        val registry = PowerBlockRegistry(TestHelper.mockPlugin)
        val batteryLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val battery = SmallBattery(batteryLoc, BlockFace.NORTH)

        val sourceLoc = TestHelper.createLocation(0.0, 64.0, 1.0)
        val source = SmallSolarPanel(sourceLoc)
        source.currentPower = 0

        TestHelper.addToRegistry(
            registry,
            battery,
            "atlas:small_battery",
        )
        TestHelper.addToRegistry(
            registry,
            source,
            "atlas:small_solar_panel",
        )

        battery.callPowerUpdate()
        assertEquals(0, battery.currentPower)
    }

    @Test
    fun `battery powerUpdate when no block behind it`() {
        val registry = PowerBlockRegistry(TestHelper.mockPlugin)
        val battery =
            SmallBattery(TestHelper.createLocation(), BlockFace.NORTH)
        TestHelper.addToRegistry(
            registry,
            battery,
            "atlas:small_battery",
        )

        battery.callPowerUpdate()
        assertEquals(0, battery.currentPower)
    }
}

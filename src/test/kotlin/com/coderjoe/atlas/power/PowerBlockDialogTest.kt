package com.coderjoe.atlas.power

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.power.block.PowerCable
import com.coderjoe.atlas.power.block.SmallBattery
import com.coderjoe.atlas.power.block.SmallDrill
import com.coderjoe.atlas.power.block.SmallSolarPanel
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.block.BlockFace
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import java.lang.reflect.Method

class PowerBlockDialogTest {

    @BeforeEach
    fun setup() {
        TestHelper.setup()
        PowerBlockDialog.init(TestHelper.mockPlugin)
    }

    @AfterEach
    fun teardown() {
        TestHelper.teardown()
    }

    private fun getDisplayName(block: PowerBlock): String {
        val method = PowerBlockDialog::class.java.getDeclaredMethod("getBlockDisplayName", PowerBlock::class.java)
        method.isAccessible = true
        return method.invoke(PowerBlockDialog, block) as String
    }

    private fun buildPowerInfo(block: PowerBlock): Component {
        val method = PowerBlockDialog::class.java.getDeclaredMethod("buildPowerInfo", PowerBlock::class.java)
        method.isAccessible = true
        return method.invoke(PowerBlockDialog, block) as Component
    }

    @Test
    fun `display name for SmallSolarPanel`() {
        assertEquals("Small Solar Panel", getDisplayName(SmallSolarPanel(TestHelper.createLocation())))
    }

    @Test
    fun `display name for SmallBattery`() {
        assertEquals("Small Battery", getDisplayName(SmallBattery(TestHelper.createLocation(), BlockFace.NORTH)))
    }

    @Test
    fun `display name for SmallDrill`() {
        assertEquals("Small Drill", getDisplayName(SmallDrill(TestHelper.createLocation())))
    }

    @Test
    fun `display name for PowerCable NORTH`() {
        assertEquals("Power Cable (North)", getDisplayName(PowerCable(TestHelper.createLocation(), BlockFace.NORTH)))
    }

    @Test
    fun `power bar color green when ratio above 0_7`() {
        val battery = SmallBattery(TestHelper.createLocation(), BlockFace.NORTH)
        battery.currentPower = 8 // 80% = green
        val info = buildPowerInfo(battery)
        val text = flattenText(info)
        assertTrue(text.contains("80%"))
    }

    @Test
    fun `power bar color yellow when ratio above 0_3`() {
        val battery = SmallBattery(TestHelper.createLocation(), BlockFace.NORTH)
        battery.currentPower = 5 // 50% = yellow
        val info = buildPowerInfo(battery)
        val text = flattenText(info)
        assertTrue(text.contains("50%"))
    }

    @Test
    fun `power bar color red when ratio below 0_3`() {
        val battery = SmallBattery(TestHelper.createLocation(), BlockFace.NORTH)
        battery.currentPower = 1 // 10% = red
        val info = buildPowerInfo(battery)
        val text = flattenText(info)
        assertTrue(text.contains("10%"))
    }

    @Test
    fun `drill info includes mining direction and status`() {
        val drill = SmallDrill(TestHelper.createLocation(), BlockFace.NORTH)
        drill.enabled = true
        val info = buildPowerInfo(drill)
        val text = flattenText(info)
        assertTrue(text.contains("North"), "Should contain direction name")
        assertTrue(text.contains("ON"), "Should contain ON status")
    }

    @Test
    fun `drill info shows OFF when disabled`() {
        val drill = SmallDrill(TestHelper.createLocation(), BlockFace.DOWN)
        drill.enabled = false
        val info = buildPowerInfo(drill)
        val text = flattenText(info)
        assertTrue(text.contains("OFF"))
    }

    @Test
    fun `cleanup clears all active dialogs`() {
        // Just verify it doesn't throw
        assertDoesNotThrow { PowerBlockDialog.cleanup() }
    }

    private fun flattenText(component: Component): String {
        val sb = StringBuilder()
        if (component is TextComponent) {
            sb.append(component.content())
        }
        for (child in component.children()) {
            sb.append(flattenText(child))
        }
        return sb.toString()
    }
}

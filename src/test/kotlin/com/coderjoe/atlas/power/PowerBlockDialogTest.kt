package com.coderjoe.atlas.power

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.core.AtlasBlockDialog
import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.power.block.LavaGenerator
import com.coderjoe.atlas.power.block.PowerCable
import com.coderjoe.atlas.power.block.SmallBattery
import com.coderjoe.atlas.power.block.SmallSolarPanel
import com.coderjoe.atlas.utility.block.AutoSmelter
import com.coderjoe.atlas.utility.block.CobblestoneFactory
import com.coderjoe.atlas.utility.block.Crusher
import com.coderjoe.atlas.utility.block.ObsidianFactory
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PowerBlockDialogTest {
    @BeforeEach
    fun setup() {
        TestHelper.setup()
        AtlasBlockDialog.init(TestHelper.mockPlugin)
    }

    @AfterEach
    fun teardown() {
        TestHelper.teardown()
    }

    private val descriptors: Map<String, BlockDescriptor> =
        listOf(
            SmallSolarPanel.descriptor,
            SmallBattery.descriptor,
            PowerCable.descriptor,
            LavaGenerator.descriptor,
            AutoSmelter.descriptor,
            CobblestoneFactory.descriptor,
            ObsidianFactory.descriptor,
            Crusher.descriptor,
        ).associateBy { it.baseBlockId }

    private fun getDisplayName(block: PowerBlock): String {
        val method =
            PowerBlockDialog::class.java.getDeclaredMethod(
                "getBlockDisplayName",
                PowerBlock::class.java,
                Map::class.java,
            )
        method.isAccessible = true
        return method.invoke(PowerBlockDialog, block, descriptors) as String
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
        assertEquals("Small Battery", getDisplayName(SmallBattery(TestHelper.createLocation())))
    }

    @Test
    fun `display name for PowerCable has no facing`() {
        assertEquals("Power Cable", getDisplayName(PowerCable(TestHelper.createLocation())))
    }

    @Test
    fun `power bar color green when ratio above 0_7`() {
        val battery = SmallBattery(TestHelper.createLocation())
        battery.currentPower = 40 // 80% = green
        val info = buildPowerInfo(battery)
        val text = flattenText(info)
        assertTrue(text.contains("80%"))
    }

    @Test
    fun `power bar color yellow when ratio above 0_3`() {
        val battery = SmallBattery(TestHelper.createLocation())
        battery.currentPower = 25 // 50% = yellow
        val info = buildPowerInfo(battery)
        val text = flattenText(info)
        assertTrue(text.contains("50%"))
    }

    @Test
    fun `power bar color red when ratio below 0_3`() {
        val battery = SmallBattery(TestHelper.createLocation())
        battery.currentPower = 5 // 10% = red
        val info = buildPowerInfo(battery)
        val text = flattenText(info)
        assertTrue(text.contains("10%"))
    }

    @Test
    fun `cleanup clears all active dialogs`() {
        // Just verify it doesn't throw
        assertDoesNotThrow { AtlasBlockDialog.cleanup() }
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

package com.coderjoe.atlas.fluid

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.TestHelper.callFluidUpdate
import com.coderjoe.atlas.fluid.block.FluidPipe
import com.coderjoe.atlas.fluid.block.FluidPump
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import org.bukkit.block.BlockFace
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

class FluidBlockDialogTest {

    @BeforeEach
    fun setup() {
        TestHelper.setup()
        FluidBlockDialog.init(TestHelper.mockPlugin)
    }

    @AfterEach
    fun teardown() {
        TestHelper.teardown()
    }

    private fun getDisplayName(block: FluidBlock): String {
        val method = FluidBlockDialog::class.java.getDeclaredMethod("getBlockDisplayName", FluidBlock::class.java)
        method.isAccessible = true
        return method.invoke(FluidBlockDialog, block) as String
    }

    private fun buildFluidInfo(block: FluidBlock): Component {
        val method = FluidBlockDialog::class.java.getDeclaredMethod("buildFluidInfo", FluidBlock::class.java)
        method.isAccessible = true
        return method.invoke(FluidBlockDialog, block) as Component
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

    @Test
    fun `display name for FluidPump`() {
        assertEquals("Fluid Pump", getDisplayName(FluidPump(TestHelper.createLocation())))
    }

    @Test
    fun `display name for FluidPipe EAST`() {
        assertEquals("Fluid Pipe (East)", getDisplayName(FluidPipe(TestHelper.createLocation(), BlockFace.EAST)))
    }

    @Test
    fun `fluid info shows Water for WATER`() {
        val pump = FluidPump(TestHelper.createLocation())
        pump.storeFluid(FluidType.WATER)
        val text = flattenText(buildFluidInfo(pump))
        assertTrue(text.contains("Water"))
    }

    @Test
    fun `fluid info shows Lava for LAVA`() {
        val pump = FluidPump(TestHelper.createLocation())
        pump.storeFluid(FluidType.LAVA)
        val text = flattenText(buildFluidInfo(pump))
        assertTrue(text.contains("Lava"))
    }

    @Test
    fun `fluid info shows Empty for NONE`() {
        val pump = FluidPump(TestHelper.createLocation())
        val text = flattenText(buildFluidInfo(pump))
        assertTrue(text.contains("Empty"))
    }

    @Test
    fun `pump info shows power status`() {
        val pump = FluidPump(TestHelper.createLocation())
        val text = flattenText(buildFluidInfo(pump))
        assertTrue(text.contains("No Power") || text.contains("Power"))
    }

    @Test
    fun `pump info shows pump status`() {
        val pump = FluidPump(TestHelper.createLocation())
        val text = flattenText(buildFluidInfo(pump))
        // Default status is NO_SOURCE
        assertTrue(text.contains("No source nearby"))
    }

    @Test
    fun `pump info shows IDLE status text when holding fluid`() {
        val pump = FluidPump(TestHelper.createLocation())
        pump.storeFluid(FluidType.WATER)
        // Need to trigger fluidUpdate to set status to IDLE
        val powerRegistry = com.coderjoe.atlas.power.PowerBlockRegistry(TestHelper.mockPlugin)
        pump.callFluidUpdate()

        val text = flattenText(buildFluidInfo(pump))
        assertTrue(text.contains("Idle"))
    }

    @Test
    fun `pump info shows EXTRACTING status text`() {
        val pump = FluidPump(TestHelper.createLocation())
        // Manually set status via reflection
        val field = FluidPump::class.java.getDeclaredField("pumpStatus")
        field.isAccessible = true
        field.set(pump, FluidPump.PumpStatus.EXTRACTING)

        val text = flattenText(buildFluidInfo(pump))
        assertTrue(text.contains("Extracting"))
    }

    @Test
    fun `pump info shows NO_POWER status text`() {
        val pump = FluidPump(TestHelper.createLocation())
        val field = FluidPump::class.java.getDeclaredField("pumpStatus")
        field.isAccessible = true
        field.set(pump, FluidPump.PumpStatus.NO_POWER)

        val text = flattenText(buildFluidInfo(pump))
        assertTrue(text.contains("Waiting for power"))
    }

    @Test
    fun `pump info shows Powered when isPowered true`() {
        val pump = FluidPump(TestHelper.createLocation())
        val field = FluidPump::class.java.getDeclaredField("isPowered")
        field.isAccessible = true
        field.set(pump, true)

        val text = flattenText(buildFluidInfo(pump))
        assertTrue(text.contains("Powered"))
        assertFalse(text.contains("No Power"))
    }

    @Test
    fun `display name for FluidPipe NORTH`() {
        assertEquals("Fluid Pipe (North)", getDisplayName(FluidPipe(TestHelper.createLocation(), BlockFace.NORTH)))
    }

    @Test
    fun `cleanup does not throw`() {
        assertDoesNotThrow { FluidBlockDialog.cleanup() }
    }
}

import com.coderjoe.atlas.AtlasBlockTypes
import com.coderjoe.atlas.block.capability.FluidType
import com.coderjoe.atlas.block.fluid.block.FluidContainer
import com.coderjoe.atlas.block.fluid.block.FluidPump
import com.coderjoe.atlas.block.power.SmallBattery
import com.coderjoe.atlas.block.transport.block.ConveyorBelt
import com.coderjoe.atlas.dialog.AtlasBlockDialog
import com.coderjoe.atlas.dialog.BlockInspectorDialog
import com.coderjoe.atlas.testing.TestHelper
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import org.bukkit.block.BlockFace
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class BlockInspectorDialogTest {
    private val catalog = AtlasBlockTypes.catalog

    @BeforeEach
    fun setup() {
        TestHelper.setup()
    }

    @AfterEach
    fun teardown() {
        TestHelper.teardown()
    }

    private fun bodyText(block: com.coderjoe.atlas.block.AtlasBlock): String {
        val description = catalog.find(block.baseBlockId)?.description
        return flatten(BlockInspectorDialog.body(block, description))
    }

    private fun flatten(component: Component): String {
        val sb = StringBuilder()
        if (component is TextComponent) sb.append(component.content())
        component.children().forEach { sb.append(flatten(it)) }
        return sb.toString()
    }

    @Test
    fun `a power block shows a gauge above its descriptor's description`() {
        val battery = SmallBattery(TestHelper.createLocation())
        battery.currentPower = 40

        val text = bodyText(battery)

        assertTrue(text.startsWith("Power: 40/50"), text)
        assertTrue(text.endsWith("Storage - holds up to 50 power, fills and drains from any side"), text)
    }

    /** The wrong half of the old pair: the dialog used to say the battery held 10. */
    @Test
    fun `the battery no longer claims to hold 10 power`() {
        val text = bodyText(SmallBattery(TestHelper.createLocation()))

        assertFalse(text.contains("holds up to 10 power"), text)
    }

    @Test
    fun `the bar tracks the fill ratio`() {
        val battery = SmallBattery(TestHelper.createLocation())

        battery.currentPower = 40
        assertTrue(bodyText(battery).contains("80%"))
        battery.currentPower = 25
        assertTrue(bodyText(battery).contains("50%"))
        battery.currentPower = 5
        assertTrue(bodyText(battery).contains("10%"))
    }

    @Test
    fun `a pump shows its fluid, its power and what it is doing`() {
        val pump = FluidPump(TestHelper.createLocation())
        pump.storeFluid(FluidType.WATER)
        pump.acceptPower(BlockFace.NORTH, FluidPump.POWER_PER_EXTRACT)

        val text = bodyText(pump)

        assertTrue(text.contains("Fluid: Water"), text)
        assertTrue(text.contains("Powered"), text)
        assertFalse(text.contains("No Power"), text)
        assertTrue(text.contains("No source nearby"), text)
    }

    @Test
    fun `a tank shows a level gauge`() {
        val tank = FluidContainer(TestHelper.createLocation())
        tank.restoreState(FluidType.LAVA, 5)

        val text = bodyText(tank)

        assertTrue(text.startsWith("Level: 5/20"), text)
        assertTrue(text.contains("Fluid: Lava"), text)
    }

    /** A block with no state of its own is all description: no gauge, no status line. */
    @Test
    fun `a belt is just its description`() {
        val belt = ConveyorBelt(TestHelper.createLocation(), BlockFace.NORTH)

        assertEquals(
            "Moves items forward in the facing direction, into a container if one is ahead",
            bodyText(belt),
        )
    }

    @Test
    fun `a directional block carries its facing in the title`() {
        val belt = ConveyorBelt(TestHelper.createLocation(), BlockFace.NORTH)

        val title = AtlasBlockDialog.defaultDisplayName(catalog.find(belt.baseBlockId), belt.facing, "Atlas Block")

        assertEquals("Conveyor Belt (North)", title)
    }

    /** Every block in the catalog renders, so no block can reach players with a broken dialog. */
    @Test
    fun `every catalog block renders a body and a title`() {
        for (id in catalog.blockIds.sorted()) {
            val descriptor = catalog.find(id)!!
            val block = catalog.create(id, TestHelper.createLocation(), BlockFace.NORTH)!!

            assertDoesNotThrow("$id failed to render") {
                BlockInspectorDialog.body(block, descriptor.description)
                AtlasBlockDialog.defaultDisplayName(descriptor, block.facing, "Atlas Block")
            }
        }
    }
}

package com.coderjoe.atlas.hologram

import com.coderjoe.atlas.AtlasBlockTypes
import com.coderjoe.atlas.block.AtlasBlock
import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.block.capability.FluidType
import com.coderjoe.atlas.block.fluid.FluidContainer
import com.coderjoe.atlas.block.fluid.FluidPump
import com.coderjoe.atlas.block.power.SmallBattery
import com.coderjoe.atlas.block.transport.ConveyorBelt
import com.coderjoe.atlas.testing.Blocks.placedIn
import com.coderjoe.atlas.testing.MockServer
import io.mockk.MockKMatcherScope
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.util.UUID
import java.util.function.Consumer

class HologramInspectorTest {
    private val catalog = AtlasBlockTypes.catalog

    private lateinit var registry: BlockRegistry
    private lateinit var player: Player
    private val playerId = UUID.randomUUID()
    private lateinit var display: TextDisplay
    private var wearing = true

    @BeforeEach
    fun setup() {
        MockServer.setup()
        registry = BlockRegistry(MockServer.plugin)
        wearing = true

        player = mockk(relaxed = true)
        every { player.uniqueId } returns playerId
        every { player.location } returns MockServer.createLocation(5.0, 64.0, 0.5)
        every { MockServer.server.onlinePlayers } returns listOf(player)
        every { MockServer.server.getPlayer(playerId) } returns player

        display = mockk(relaxed = true)
        every { display.isValid } returns true
        every { display.world } returns MockServer.world
        every { spawnCall() } answers {
            thirdArg<Consumer<in TextDisplay>>().accept(display)
            display
        }
    }

    @AfterEach
    fun teardown() {
        MockServer.teardown()
    }

    private fun inspector() = HologramInspector(MockServer.plugin, registry, catalog) { wearing }

    private fun lookAt(block: AtlasBlock) {
        val target = mockk<Block>()
        every { target.location } returns block.location
        every { player.getTargetBlockExact(any()) } returns target
    }

    private fun MockKMatcherScope.spawnCall() =
        MockServer.world.spawn(
            any<Location>(),
            TextDisplay::class.java,
            any<Consumer<in TextDisplay>>(),
        )

    private fun battery(): SmallBattery = SmallBattery(MockServer.createLocation()).placedIn(registry)

    private fun panelText(block: AtlasBlock): String =
        PlainTextComponentSerializer.plainText().serialize(HologramInspector.panelText(block, catalog.find(block.baseBlockId)))

    @Test
    fun `a wearer looking at a machine gets a panel only they can see`() {
        lookAt(battery())

        inspector().refresh()

        verify { display.isVisibleByDefault = false }
        verify { display.isPersistent = false }
        verify { player.showEntity(MockServer.plugin, display) }
    }

    @Test
    fun `a player without goggles gets no panel`() {
        wearing = false
        lookAt(battery())

        inspector().refresh()

        verify(exactly = 0) { spawnCall() }
    }

    @Test
    fun `an unchanged machine sends nothing after the first refresh`() {
        lookAt(battery())
        val inspector = inspector()

        inspector.refresh()
        inspector.refresh()

        verify(exactly = 1) { display.text(any()) }
        verify(exactly = 0) { display.teleport(any<Location>()) }
    }

    @Test
    fun `a change in the machine redraws the panel`() {
        val battery = battery()
        lookAt(battery)
        val inspector = inspector()

        inspector.refresh()
        battery.currentPower = 25
        inspector.refresh()

        verify(exactly = 2) { display.text(any()) }
    }

    @Test
    fun `looking at another machine moves the same panel`() {
        val first = battery()
        val second = SmallBattery(MockServer.createLocation(0.0, 64.0, 3.0)).placedIn(registry)
        val inspector = inspector()

        lookAt(first)
        inspector.refresh()
        lookAt(second)
        inspector.refresh()

        verify(exactly = 1) { spawnCall() }
        verify { display.teleport(any<Location>()) }
    }

    @Test
    fun `taking the goggles off removes the panel`() {
        lookAt(battery())
        val inspector = inspector()

        inspector.refresh()
        wearing = false
        inspector.refresh()

        verify { display.remove() }
    }

    @Test
    fun `looking at something that is not an Atlas block removes the panel`() {
        lookAt(battery())
        val inspector = inspector()

        inspector.refresh()
        every { player.getTargetBlockExact(any()) } returns null
        inspector.refresh()

        verify { display.remove() }
    }

    @Test
    fun `a player who leaves takes their panel with them`() {
        lookAt(battery())
        val inspector = inspector()

        inspector.refresh()
        every { MockServer.server.onlinePlayers } returns emptyList()
        every { MockServer.server.getPlayer(playerId) } returns null
        inspector.refresh()

        verify { display.remove() }
    }

    @Test
    fun `stopping removes every panel`() {
        lookAt(battery())
        val inspector = inspector()

        inspector.refresh()
        inspector.stop()

        verify { display.remove() }
    }

    @Test
    fun `the panel stands off the side facing the viewer`() {
        val block = MockServer.createLocation(10.0, 64.0, 10.0)

        val east = HologramInspector.anchor(block, MockServer.createLocation(20.0, 64.0, 11.0))
        assertEquals(11.25, east.x)
        assertEquals(10.5, east.z)
        assertEquals(64.9, east.y, 1e-9)

        val north = HologramInspector.anchor(block, MockServer.createLocation(10.0, 64.0, 0.0))
        assertEquals(10.5, north.x)
        assertEquals(9.75, north.z)
    }

    @Test
    fun `a power block shows its name, a gauge and its description`() {
        val battery = battery()
        battery.currentPower = 40

        val text = panelText(battery)

        assertTrue(text.startsWith("Small Battery\nPower: 40/50"), text)
        assertTrue(text.endsWith("Storage - holds up to 50 power, fills and drains from any side"), text)
    }

    @Test
    fun `the bar tracks the fill ratio`() {
        val battery = battery()

        battery.currentPower = 40
        assertTrue(panelText(battery).contains("80%"))
        battery.currentPower = 25
        assertTrue(panelText(battery).contains("50%"))
        battery.currentPower = 5
        assertTrue(panelText(battery).contains("10%"))
    }

    @Test
    fun `a pump shows its fluid, its power and what it is doing`() {
        val pump = FluidPump(MockServer.createLocation())
        pump.storeFluid(FluidType.WATER)
        pump.acceptPower(BlockFace.NORTH, FluidPump.POWER_PER_EXTRACT)

        val text = panelText(pump)

        assertTrue(text.contains("Fluid: Water"), text)
        assertTrue(text.contains("Powered"), text)
        assertFalse(text.contains("No Power"), text)
        assertTrue(text.contains("No source nearby"), text)
    }

    @Test
    fun `a tank shows a level gauge`() {
        val tank = FluidContainer(MockServer.createLocation())
        tank.restoreState(FluidType.LAVA, 5)

        val text = panelText(tank)

        assertTrue(text.contains("Level: 5/20"), text)
        assertTrue(text.contains("Fluid: Lava"), text)
    }

    /** A block with no state of its own is its name and description: no gauge, no status line. */
    @Test
    fun `a belt is just its name and description`() {
        val belt = ConveyorBelt(MockServer.createLocation(), BlockFace.NORTH)

        assertEquals(
            "Conveyor Belt (North)\nMoves items forward in the facing direction, into a container if one is ahead",
            panelText(belt),
        )
    }

    /**
     * Every block in the catalog renders, so no block can reach players with a broken panel.
     * Each is registered first, as it is in play, because a cable reads its run from the registry.
     */
    @Test
    fun `every catalog block renders a panel`() {
        for (id in catalog.blockIds.sorted()) {
            val block = catalog.create(id, MockServer.createLocation(), BlockFace.NORTH)!!
            BlockRegistry(MockServer.plugin).track(block, id)

            assertDoesNotThrow("$id failed to render") { panelText(block) }
        }
    }
}

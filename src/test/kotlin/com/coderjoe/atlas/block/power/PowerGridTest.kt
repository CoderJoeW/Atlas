package com.coderjoe.atlas.block.power

import com.coderjoe.atlas.block.AtlasBlock
import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.block.capability.PowerConsumer
import com.coderjoe.atlas.craftengine.CraftEngineHelper
import com.coderjoe.atlas.testing.MockServer
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.runs
import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PowerGridTest {
    private lateinit var registry: BlockRegistry

    /** Takes one unit per server tick at most, and remembers the ticks it was fed on. */
    private class Meter(location: Location) : AtlasBlock(location), PowerConsumer {
        val fedOn = mutableListOf<Long>()

        override fun drawsPowerFrom(face: BlockFace): Boolean = true

        override fun wantsPower(): Boolean = fedOn.lastOrNull() != registry.ticker.currentTick

        override fun acceptPower(
            face: BlockFace,
            amount: Int,
        ): Int {
            fedOn += registry.ticker.currentTick
            return 1
        }

        override fun blockUpdate() {}

        override fun getVisualStateBlockId(): String = "atlas:meter"
    }

    @BeforeEach
    fun setup() {
        MockServer.setup()
        mockkObject(CraftEngineHelper)
        every { CraftEngineHelper.placeState(any(), any()) } just runs
        registry = BlockRegistry(MockServer.plugin)
    }

    @AfterEach
    fun teardown() {
        MockServer.teardown()
    }

    private fun cable(z: Int): PowerCable =
        PowerCable(MockServer.createLocation(0.0, 64.0, z.toDouble())).also { registry.track(it, PowerCable.BLOCK_ID) }

    private fun cables(range: IntRange) = range.map(::cable)

    private fun layCables(length: Int) {
        for (z in 0 until length) {
            registry.register(PowerCable(MockServer.createLocation(0.0, 64.0, z.toDouble())), PowerCable.BLOCK_ID)
        }
    }

    @Test
    fun `every cable in a run shares one network until the run changes`() {
        val run = cables(0..9)
        val network = PowerNetworks.networkFor(run[0])
        val lookupsAfterBuild = registry.adjacentLookups

        assertTrue(run.all { PowerNetworks.networkFor(it) === network })
        run.forEach { it.canSupplyPower() }
        run.forEach { it.inspect() }
        assertEquals(lookupsAfterBuild, registry.adjacentLookups, "asking again must not walk the run")
    }

    @Test
    fun `breaking a cable splits the run and placing it back merges it`() {
        val run = cables(0..4)
        val whole = PowerNetworks.networkFor(run[0])

        registry.unregister(run[2].location)
        val left = PowerNetworks.networkFor(run[0])
        val right = PowerNetworks.networkFor(run[4])
        assertNotSame(whole, left)
        assertNotSame(left, right)
        assertEquals(listOf(2, 2), listOf(left.cables.size, right.cables.size))

        val bridge = cable(2)
        val merged = PowerNetworks.networkFor(bridge)
        assertSame(merged, PowerNetworks.networkFor(run[0]))
        assertSame(merged, PowerNetworks.networkFor(run[4]))
        assertEquals(5, merged.cables.size)
    }

    @Test
    fun `a block placed or broken against a cached run is seen by it`() {
        val run = cables(0..2)
        val panel = SmallSolarPanel(MockServer.createLocation(0.0, 65.0, 0.0))
        panel.currentPower = 4
        registry.track(panel, SmallSolarPanel.BLOCK_ID)
        assertTrue(run[2].canSupplyPower(), "the cached run picks up a panel placed against it")

        val battery = SmallBattery(MockServer.createLocation(0.0, 64.0, 3.0))
        registry.track(battery, SmallBattery.BLOCK_ID)
        PowerNetworks.networkFor(run[0]).tick()
        assertEquals(4, battery.currentPower, "and a battery placed at the far end")

        registry.unregister(panel.location)
        assertTrue(PowerNetworks.networkFor(run[0]).terminals().first.single().block === battery)
    }

    @Test
    fun `a network ticks once per interval however many cables it has`() {
        for (length in listOf(3, 60)) {
            MockServer.teardown()
            setup()
            val battery = SmallBattery(MockServer.createLocation(0.0, 64.0, -1.0))
            battery.currentPower = battery.maxStorage
            registry.register(battery, SmallBattery.BLOCK_ID)
            layCables(length)
            val meter = Meter(MockServer.createLocation(0.0, 64.0, length.toDouble()))
            registry.register(meter, "atlas:meter")

            repeat(3 * PowerGrid.TICK_INTERVAL.toInt()) { registry.ticker.tick() }

            assertEquals(3, meter.fedOn.size, "a run of $length cables should transfer once per interval, fed on ${meter.fedOn}")
            assertTrue(meter.fedOn.zipWithNext().all { (a, b) -> b - a == PowerGrid.TICK_INTERVAL })
        }
    }

    @Test
    fun `a full interval of a run costs neighbour lookups linear in its length`() {
        fun lookupsFor(length: Int): Long {
            MockServer.teardown()
            setup()
            val panel = SmallSolarPanel(MockServer.createLocation(0.0, 65.0, 0.0))
            panel.currentPower = panel.maxStorage
            registry.register(panel, SmallSolarPanel.BLOCK_ID)
            layCables(length)
            registry.register(SmallBattery(MockServer.createLocation(0.0, 64.0, length.toDouble())), SmallBattery.BLOCK_ID)

            val before = registry.adjacentLookups
            repeat(PowerGrid.TICK_INTERVAL.toInt()) { registry.ticker.tick() }
            return registry.adjacentLookups - before
        }

        val small = lookupsFor(50)
        val large = lookupsFor(400)

        assertTrue(large <= 15 * 400, "expected a handful of lookups per cable, got $large for 400 cables")
        assertTrue(large < small * 10, "8x the cable should cost about 8x the lookups, not 64x: $small then $large")
    }
}

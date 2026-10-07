package com.coderjoe.atlas.block.fluid

import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.block.capability.FluidType
import com.coderjoe.atlas.block.power.LavaGenerator
import com.coderjoe.atlas.craftengine.CraftEngineHelper
import com.coderjoe.atlas.testing.MockServer
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.runs
import org.bukkit.block.BlockFace
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FluidGridTest {
    private lateinit var registry: BlockRegistry

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

    private fun pipe(z: Int): FluidPipe =
        FluidPipe(MockServer.createLocation(0.0, 64.0, z.toDouble())).also { registry.track(it, FluidPipe.BLOCK_ID) }

    private fun tank(
        x: Int,
        z: Int,
        fluid: FluidType = FluidType.NONE,
        amount: Int = 0,
    ): FluidContainer =
        FluidContainer(MockServer.createLocation(x.toDouble(), 64.0, z.toDouble())).also {
            registry.track(it, FluidContainer.BLOCK_ID)
            repeat(amount) { _ -> it.storeFluid(fluid) }
        }

    @Test
    fun `lava and water stay separate where they meet, and each side ticks on its own`() {
        val pipes = (0..3).map(::pipe)
        val lava = tank(0, -1, FluidType.LAVA, 5)
        val water = tank(0, 4, FluidType.WATER, 5)
        val lavaSink = tank(1, 0)
        val waterSink = tank(1, 3)

        val lavaRun = FluidNetworks.networkFor(pipes[0])
        val waterRun = FluidNetworks.networkFor(pipes[3])
        assertNotSame(lavaRun, waterRun)
        assertSame(lavaRun, FluidNetworks.networkFor(pipes[1]))
        assertSame(waterRun, FluidNetworks.networkFor(pipes[2]))

        FluidGrid.of(registry).tick()

        assertEquals(FluidType.LAVA, lavaSink.storedFluid)
        assertEquals(FluidType.WATER, waterSink.storedFluid)
        assertEquals(4, lava.storedAmount)
        assertEquals(4, water.storedAmount)
        assertEquals(FluidType.LAVA, pipes[1].carrying)
        assertEquals(FluidType.WATER, pipes[2].carrying)
    }

    @Test
    fun `the boundary moves when a source empties or refills, with nothing placed or broken`() {
        val pipes = (0..3).map(::pipe)
        tank(0, -1, FluidType.LAVA, 1)
        val water = tank(0, 4, FluidType.WATER, 1)
        assertNotSame(FluidNetworks.networkFor(pipes[0]), FluidNetworks.networkFor(pipes[3]))

        water.removeFluid()
        val whole = FluidNetworks.networkFor(pipes[0])
        assertEquals(4, whole.pipes.size, "with the water gone the lava reaches the whole run")
        assertSame(whole, FluidNetworks.networkFor(pipes[3]))

        water.storeFluid(FluidType.WATER)
        assertEquals(2, FluidNetworks.networkFor(pipes[3]).pipes.size, "refilled, the water side splits off again")
    }

    @Test
    fun `breaking a pipe splits the run and placing it back merges it`() {
        val pipes = (0..4).map(::pipe)
        val whole = FluidNetworks.networkFor(pipes[0])

        registry.unregister(pipes[2].location)
        val left = FluidNetworks.networkFor(pipes[0])
        val right = FluidNetworks.networkFor(pipes[4])
        assertNotSame(whole, left)
        assertNotSame(left, right)
        assertEquals(listOf(2, 2), listOf(left.pipes.size, right.pipes.size))

        val bridge = pipe(2)
        assertSame(FluidNetworks.networkFor(bridge), FluidNetworks.networkFor(pipes[0]))
        assertSame(FluidNetworks.networkFor(bridge), FluidNetworks.networkFor(pipes[4]))
        assertEquals(5, FluidNetworks.networkFor(bridge).pipes.size)
    }

    @Test
    fun `asking a pipe about its run does not walk the run again`() {
        val pipes = (0..9).map(::pipe)
        tank(0, -1, FluidType.LAVA, 3)
        FluidNetworks.networkFor(pipes[0])
        val lookups = registry.adjacentLookups

        for (pipe in pipes) {
            pipe.hasFluid()
            pipe.canAcceptFluid(BlockFace.NORTH, FluidType.LAVA)
        }

        assertEquals(lookups, registry.adjacentLookups)
    }

    @Test
    fun `a network moves one unit per interval however many pipes it has`() {
        for (length in listOf(2, 50)) {
            MockServer.teardown()
            setup()
            val source = FluidContainer(MockServer.createLocation(0.0, 64.0, -1.0))
            registry.register(source, FluidContainer.BLOCK_ID)
            repeat(10) { source.storeFluid(FluidType.LAVA) }
            for (z in 0 until length) {
                registry.register(FluidPipe(MockServer.createLocation(0.0, 64.0, z.toDouble())), FluidPipe.BLOCK_ID)
            }
            registry.register(LavaGenerator(MockServer.createLocation(0.0, 64.0, length.toDouble())), "atlas:lava_generator")

            repeat(3 * FluidGrid.TICK_INTERVAL.toInt()) { registry.ticker.tick() }

            assertEquals(7, source.storedAmount, "a run of $length pipes should move exactly three units in three intervals")
        }
    }
}

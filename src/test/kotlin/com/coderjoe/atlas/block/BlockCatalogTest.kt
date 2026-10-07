package com.coderjoe.atlas.block

import com.coderjoe.atlas.block.fluid.FluidPump
import com.coderjoe.atlas.block.power.SmallBattery
import com.coderjoe.atlas.block.power.SmallSolarPanel
import com.coderjoe.atlas.block.transport.ConveyorBelt
import com.coderjoe.atlas.testing.MockServer
import org.bukkit.block.BlockFace
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BlockCatalogTest {
    private val catalog = BlockCatalog(listOf(SmallSolarPanel.descriptor, SmallBattery.descriptor, FluidPump.descriptor))

    @BeforeEach
    fun setup() {
        MockServer.setup()
    }

    @AfterEach
    fun teardown() {
        MockServer.teardown()
    }

    @Test
    fun `create builds the block its descriptor names, whatever the family`() {
        assertInstanceOf(SmallSolarPanel::class.java, catalog.create(SmallSolarPanel.descriptor.baseBlockId, MockServer.createLocation()))
        assertInstanceOf(FluidPump::class.java, catalog.create(FluidPump.descriptor.baseBlockId, MockServer.createLocation()))
    }

    @Test
    fun `create returns null and find returns null for an unknown id`() {
        assertNull(catalog.create("unknown", MockServer.createLocation()))
        assertNull(catalog.find("unknown"))
    }

    @Test
    fun `additional ids resolve to the descriptor that declares them`() {
        for (id in SmallBattery.descriptor.additionalBlockIds) {
            assertSame(SmallBattery.descriptor, catalog.find(id), id)
            assertInstanceOf(SmallBattery::class.java, catalog.create(id, MockServer.createLocation()), id)
        }
    }

    @Test
    fun `blockIds lists every base and additional id`() {
        val expected =
            listOf(SmallSolarPanel.descriptor, SmallBattery.descriptor, FluidPump.descriptor)
                .flatMap { listOf(it.baseBlockId) + it.additionalBlockIds }
                .toSet()
        assertEquals(expected, catalog.blockIds)
    }

    @Test
    fun `create hands the facing to the block`() {
        val belt = BlockCatalog(listOf(ConveyorBelt.descriptor)).create(ConveyorBelt.BLOCK_ID, MockServer.createLocation(), BlockFace.NORTH)
        assertEquals(BlockFace.NORTH, assertInstanceOf(ConveyorBelt::class.java, belt).facing)
    }

    @Test
    fun `two descriptors claiming the same id are rejected`() {
        val clash = SmallBattery.descriptor.copy(baseBlockId = SmallSolarPanel.descriptor.baseBlockId)
        assertThrows<IllegalArgumentException> { BlockCatalog(listOf(SmallSolarPanel.descriptor, clash)) }
    }
}

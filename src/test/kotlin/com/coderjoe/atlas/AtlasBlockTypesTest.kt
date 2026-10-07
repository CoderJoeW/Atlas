package com.coderjoe.atlas

import com.coderjoe.atlas.block.AtlasBlock
import com.coderjoe.atlas.block.fluid.FluidBlock
import com.coderjoe.atlas.block.fluid.FluidPipe
import com.coderjoe.atlas.block.fluid.FluidPump
import com.coderjoe.atlas.block.power.PowerBlock
import com.coderjoe.atlas.block.power.PowerCable
import com.coderjoe.atlas.block.power.SmallBattery
import com.coderjoe.atlas.block.power.SmallSolarPanel
import com.coderjoe.atlas.block.power.mine.MineTier
import com.coderjoe.atlas.block.transport.ConveyorBelt
import com.coderjoe.atlas.block.transport.TransportBlock
import com.coderjoe.atlas.testing.MockServer
import org.bukkit.block.BlockFace
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AtlasBlockTypesTest {
    private val catalog = AtlasBlockTypes.catalog

    @BeforeEach
    fun setup() {
        MockServer.setup()
    }

    @AfterEach
    fun teardown() {
        MockServer.teardown()
    }

    private inline fun <reified T : AtlasBlock> idsOfFamily(): List<String> =
        catalog.blockIds.filter { catalog.create(it, MockServer.createLocation()) is T }

    @Test
    fun `power family has its twelve machine ids plus one per mine`() {
        // Solar panel 2, battery 5, cable 1, lava generator 2, cobblestone factory 1, obsidian factory 1, mines 1 each
        assertEquals(12 + MineTier.entries.size, idsOfFamily<PowerBlock>().size)
    }

    @Test
    fun `fluid family has its pump, pipe and container`() {
        assertEquals(3, idsOfFamily<FluidBlock>().size)
    }

    @Test
    fun `transport family has the conveyor belt`() {
        assertEquals(listOf(ConveyorBelt.BLOCK_ID), idsOfFamily<TransportBlock>())
    }

    @Test
    fun `every id belongs to exactly one family`() {
        val families = idsOfFamily<PowerBlock>() + idsOfFamily<FluidBlock>() + idsOfFamily<TransportBlock>()
        assertEquals(catalog.blockIds, families.toSet())
        assertEquals(catalog.blockIds.size, families.size)
    }

    @Test
    fun `battery base and variant ids are all catalogued`() {
        val ids = listOf("atlas:small_battery", "atlas:small_battery_low", "atlas:small_battery_medium", "atlas:small_battery_full")
        for (id in ids) {
            assertNotNull(catalog.find(id), id)
        }
    }

    @Test
    fun `each id creates its own block type`() {
        val location = MockServer.createLocation()
        assertInstanceOf(SmallSolarPanel::class.java, catalog.create("atlas:small_solar_panel", location))
        assertInstanceOf(SmallBattery::class.java, catalog.create("atlas:small_battery", location, BlockFace.DOWN))
        assertInstanceOf(PowerCable::class.java, catalog.create("atlas:power_cable", location, BlockFace.NORTH))
        assertInstanceOf(FluidPump::class.java, catalog.create(FluidPump.BLOCK_ID, location))
        assertInstanceOf(FluidPipe::class.java, catalog.create(FluidPipe.BLOCK_ID, location, BlockFace.NORTH))
    }

    @Test
    fun `every mine tier is catalogued under its own id`() {
        for (tier in MineTier.entries) {
            assertSame(tier.descriptor, catalog.find(tier.descriptor.baseBlockId), tier.name)
        }
    }
}

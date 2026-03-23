package com.coderjoe.atlas.utility

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.TestHelper.callPowerUpdate
import com.coderjoe.atlas.core.PlacementType
import com.coderjoe.atlas.power.PowerBlockFactory
import com.coderjoe.atlas.power.PowerBlockRegistry
import com.coderjoe.atlas.utility.block.Crusher
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.entity.Item
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture

class CrusherTest {
    @BeforeEach
    fun setup() {
        TestHelper.setup()
    }

    @AfterEach
    fun teardown() {
        TestHelper.teardown()
    }

    @Test
    fun `crusher has correct facing`() {
        val crusher = Crusher(TestHelper.createLocation(), BlockFace.NORTH)
        assertEquals(BlockFace.NORTH, crusher.facing)
    }

    @Test
    fun `crusher visual state always returns BLOCK_ID`() {
        val crusher = Crusher(TestHelper.createLocation(), BlockFace.NORTH)
        crusher.currentPower = 0
        assertEquals("atlas:crusher", crusher.getVisualStateBlockId())
        crusher.currentPower = 4
        assertEquals("atlas:crusher", crusher.getVisualStateBlockId())
    }

    @Test
    fun `crusher base block ID is atlas crusher`() {
        val crusher = Crusher(TestHelper.createLocation(), BlockFace.SOUTH)
        assertEquals("atlas:crusher", crusher.baseBlockId)
    }

    @Test
    fun `crusher descriptor has correct properties`() {
        val desc = Crusher.descriptor
        assertEquals("atlas:crusher", desc.baseBlockId)
        assertEquals("Crusher", desc.displayName)
        assertEquals(PlacementType.DIRECTIONAL, desc.placementType)
    }

    @Test
    fun `max storage is 8`() {
        val crusher = Crusher(TestHelper.createLocation(), BlockFace.NORTH)
        assertEquals(8, crusher.maxStorage)
    }

    @Test
    fun `canReceivePower is true`() {
        val crusher = Crusher(TestHelper.createLocation(), BlockFace.NORTH)
        assertTrue(crusher.canAcceptPower())
    }

    @Test
    fun `base ID is registered`() {
        TestHelper.initPowerFactory()
        assertTrue(PowerBlockFactory.isRegistered("atlas:crusher"))
    }

    @Test
    fun `factory creates Crusher from base ID`() {
        TestHelper.initPowerFactory()
        val block =
            PowerBlockFactory.createPowerBlock(
                "atlas:crusher",
                TestHelper.createLocation(),
                BlockFace.NORTH,
            )
        assertTrue(block is Crusher)
        assertEquals(BlockFace.NORTH, block!!.facing)
    }

    @Test
    fun `power update does not throw with no nearby entities`() {
        PowerBlockRegistry(TestHelper.mockPlugin)
        val crusher = Crusher(TestHelper.createLocation(), BlockFace.NORTH)

        every {
            TestHelper.mockWorld.getNearbyEntities(
                any<Location>(), any(), any(), any(),
            )
        } returns emptyList()

        assertDoesNotThrow {
            crusher.callPowerUpdate()
        }
    }

    @Test
    fun `power update moves item forward without crushing when no power`() {
        PowerBlockRegistry(TestHelper.mockPlugin)
        val crusher = Crusher(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.NORTH)
        crusher.currentPower = 0

        val itemLoc = Location(TestHelper.mockWorld, 0.5, 64.375, 0.5)
        val mockItem = mockk<Item>(relaxed = true)
        val mockStack = mockk<ItemStack>(relaxed = true)
        every { mockItem.location } returns itemLoc
        every { mockItem.itemStack } returns mockStack
        every { mockStack.type } returns Material.IRON_ORE
        every { mockItem.teleportAsync(any()) } returns CompletableFuture.completedFuture(true)

        every {
            TestHelper.mockWorld.getNearbyEntities(
                any<Location>(), any(), any(), any(),
            )
        } returns listOf(mockItem)

        crusher.callPowerUpdate()

        verify {
            mockItem.teleportAsync(match { loc -> loc.z < 0.5 && loc.x == 0.5 })
        }
        assertEquals(0, crusher.currentPower)
    }

    @Test
    fun `power update moves item east`() {
        PowerBlockRegistry(TestHelper.mockPlugin)
        val crusher = Crusher(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.EAST)
        crusher.currentPower = 0

        val itemLoc = Location(TestHelper.mockWorld, 0.5, 64.375, 0.5)
        val mockItem = mockk<Item>(relaxed = true)
        val mockStack = mockk<ItemStack>(relaxed = true)
        every { mockItem.location } returns itemLoc
        every { mockItem.itemStack } returns mockStack
        every { mockStack.type } returns Material.STONE
        every { mockItem.teleportAsync(any()) } returns CompletableFuture.completedFuture(true)

        every {
            TestHelper.mockWorld.getNearbyEntities(
                any<Location>(), any(), any(), any(),
            )
        } returns listOf(mockItem)

        crusher.callPowerUpdate()

        verify {
            mockItem.teleportAsync(match { loc -> loc.x > 0.5 && loc.z == 0.5 })
        }
    }

    @Test
    fun `crushes ore block item and consumes power`() {
        PowerBlockRegistry(TestHelper.mockPlugin)
        val crusher = Crusher(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.NORTH)
        crusher.currentPower = 4

        val itemLoc = Location(TestHelper.mockWorld, 0.5, 64.375, 0.5)
        val mockItem = mockk<Item>(relaxed = true)
        val mockStack = mockk<ItemStack>(relaxed = true)
        every { mockItem.location } returns itemLoc
        every { mockItem.itemStack } returns mockStack
        every { mockStack.type } returns Material.IRON_ORE
        every { mockStack.amount } returns 1
        every { mockItem.teleportAsync(any()) } returns CompletableFuture.completedFuture(true)

        every {
            TestHelper.mockWorld.getNearbyEntities(
                any<Location>(), any(), any(), any(),
            )
        } returns listOf(mockItem)

        try {
            crusher.callPowerUpdate()
        } catch (_: Throwable) {
            // ItemStack constructor triggers Registry init
        }

        assertEquals(0, crusher.currentPower)
    }

    @Test
    fun `does not crush non-ore items`() {
        PowerBlockRegistry(TestHelper.mockPlugin)
        val crusher = Crusher(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.NORTH)
        crusher.currentPower = 4

        val itemLoc = Location(TestHelper.mockWorld, 0.5, 64.375, 0.5)
        val mockItem = mockk<Item>(relaxed = true)
        val mockStack = mockk<ItemStack>(relaxed = true)
        every { mockItem.location } returns itemLoc
        every { mockItem.itemStack } returns mockStack
        every { mockStack.type } returns Material.COBBLESTONE
        every { mockItem.teleportAsync(any()) } returns CompletableFuture.completedFuture(true)

        every {
            TestHelper.mockWorld.getNearbyEntities(
                any<Location>(), any(), any(), any(),
            )
        } returns listOf(mockItem)

        crusher.callPowerUpdate()

        assertEquals(4, crusher.currentPower)
    }

    @Test
    fun `ore to drop mapping covers all ore types`() {
        val mappings = Crusher.ORE_TO_DROP
        assertTrue(mappings.containsKey(Material.IRON_ORE))
        assertTrue(mappings.containsKey(Material.GOLD_ORE))
        assertTrue(mappings.containsKey(Material.COPPER_ORE))
        assertTrue(mappings.containsKey(Material.COAL_ORE))
        assertTrue(mappings.containsKey(Material.DIAMOND_ORE))
        assertTrue(mappings.containsKey(Material.EMERALD_ORE))
        assertTrue(mappings.containsKey(Material.LAPIS_ORE))
        assertTrue(mappings.containsKey(Material.REDSTONE_ORE))
        assertTrue(mappings.containsKey(Material.DEEPSLATE_IRON_ORE))
        assertTrue(mappings.containsKey(Material.DEEPSLATE_GOLD_ORE))
        assertTrue(mappings.containsKey(Material.DEEPSLATE_COPPER_ORE))
        assertTrue(mappings.containsKey(Material.DEEPSLATE_COAL_ORE))
        assertTrue(mappings.containsKey(Material.DEEPSLATE_DIAMOND_ORE))
        assertTrue(mappings.containsKey(Material.DEEPSLATE_EMERALD_ORE))
        assertTrue(mappings.containsKey(Material.DEEPSLATE_LAPIS_ORE))
        assertTrue(mappings.containsKey(Material.DEEPSLATE_REDSTONE_ORE))
    }

    @Test
    fun `iron ore drops raw iron`() {
        assertEquals(Material.RAW_IRON, Crusher.ORE_TO_DROP[Material.IRON_ORE])
    }

    @Test
    fun `gold ore drops raw gold`() {
        assertEquals(Material.RAW_GOLD, Crusher.ORE_TO_DROP[Material.GOLD_ORE])
    }

    @Test
    fun `copper ore drops raw copper`() {
        assertEquals(Material.RAW_COPPER, Crusher.ORE_TO_DROP[Material.COPPER_ORE])
    }

    @Test
    fun `coal ore drops coal`() {
        assertEquals(Material.COAL, Crusher.ORE_TO_DROP[Material.COAL_ORE])
    }

    @Test
    fun `diamond ore drops diamond`() {
        assertEquals(Material.DIAMOND, Crusher.ORE_TO_DROP[Material.DIAMOND_ORE])
    }

    @Test
    fun `pulls power from adjacent blocks`() {
        val registry = PowerBlockRegistry(TestHelper.mockPlugin)
        val crusherLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val crusher = Crusher(crusherLoc, BlockFace.NORTH)
        TestHelper.addToRegistry(registry, crusher, "atlas:crusher")

        val batteryLoc = TestHelper.createLocation(1.0, 64.0, 0.0)
        val battery = com.coderjoe.atlas.power.block.SmallBattery(batteryLoc, BlockFace.WEST)
        battery.currentPower = 5
        TestHelper.addToRegistry(registry, battery, "atlas:small_battery")

        every {
            TestHelper.mockWorld.getNearbyEntities(
                any<Location>(), any(), any(), any(),
            )
        } returns emptyList()

        crusher.callPowerUpdate()

        assertTrue(crusher.currentPower > 0)
        assertTrue(battery.currentPower < 5)
    }

    @Test
    fun `does not exceed max storage when pulling power`() {
        val registry = PowerBlockRegistry(TestHelper.mockPlugin)
        val crusherLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val crusher = Crusher(crusherLoc, BlockFace.NORTH)
        crusher.currentPower = 8
        TestHelper.addToRegistry(registry, crusher, "atlas:crusher")

        val batteryLoc = TestHelper.createLocation(1.0, 64.0, 0.0)
        val battery = com.coderjoe.atlas.power.block.SmallBattery(batteryLoc, BlockFace.WEST)
        battery.currentPower = 5
        TestHelper.addToRegistry(registry, battery, "atlas:small_battery")

        every {
            TestHelper.mockWorld.getNearbyEntities(
                any<Location>(), any(), any(), any(),
            )
        } returns emptyList()

        crusher.callPowerUpdate()

        assertEquals(8, crusher.currentPower)
        assertEquals(5, battery.currentPower)
    }
}

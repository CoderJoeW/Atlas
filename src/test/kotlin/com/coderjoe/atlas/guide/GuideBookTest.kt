package com.coderjoe.atlas.guide

import com.coderjoe.atlas.TestHelper
import io.mockk.*
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
import org.bukkit.World
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

class GuideBookTest {

    @BeforeEach
    fun setup() {
        TestHelper.setup()
    }

    @AfterEach
    fun teardown() {
        TestHelper.teardown()
    }

    @Test
    fun `buildPages produces 14 pages`() {
        val pages = GuideBook.buildPages()
        assertEquals(14, pages.size)
    }

    @Test
    fun `giveToPlayer adds to inventory when space available`() {
        mockkObject(GuideBook)
        val mockBook = mockk<ItemStack>(relaxed = true)
        every { GuideBook.create() } returns mockBook

        val player = mockk<Player>(relaxed = true)
        val inventory = mockk<PlayerInventory>(relaxed = true)
        every { player.inventory } returns inventory
        every { inventory.firstEmpty() } returns 0
        every { inventory.addItem(any<ItemStack>()) } returns HashMap()

        GuideBook.giveToPlayer(player)

        verify(exactly = 1) { inventory.addItem(mockBook) }
        verify(exactly = 0) { player.world.dropItem(any(), any()) }

        unmockkObject(GuideBook)
    }

    @Test
    fun `giveToPlayer drops item when inventory full`() {
        mockkObject(GuideBook)
        val mockBook = mockk<ItemStack>(relaxed = true)
        every { GuideBook.create() } returns mockBook

        val player = mockk<Player>(relaxed = true)
        val inventory = mockk<PlayerInventory>(relaxed = true)
        val world = mockk<World>(relaxed = true)
        every { player.inventory } returns inventory
        every { inventory.firstEmpty() } returns -1
        every { player.world } returns world

        GuideBook.giveToPlayer(player)

        verify(exactly = 0) { inventory.addItem(any<ItemStack>()) }
        verify(exactly = 1) { world.dropItem(any(), mockBook) }
        verify(exactly = 1) { player.sendMessage(any<Component>()) }

        unmockkObject(GuideBook)
    }
}

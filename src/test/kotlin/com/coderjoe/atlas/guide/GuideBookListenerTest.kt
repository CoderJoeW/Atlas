package com.coderjoe.atlas.guide

import com.coderjoe.atlas.TestHelper
import io.mockk.*
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
import org.junit.jupiter.api.*
import java.util.UUID

class GuideBookListenerTest {

    @BeforeEach
    fun setup() {
        TestHelper.setup()
        mockkObject(GuideBook)
        every { GuideBook.giveToPlayer(any()) } just Runs
    }

    @AfterEach
    fun teardown() {
        unmockkObject(GuideBook)
        TestHelper.teardown()
    }

    @Test
    fun `first join gives book`() {
        val listener = GuideBookListener(TestHelper.mockPlugin)

        val player = mockk<Player>(relaxed = true)
        every { player.uniqueId } returns UUID.randomUUID()

        val event = mockk<PlayerJoinEvent>(relaxed = true)
        every { event.player } returns player

        listener.onPlayerJoin(event)

        verify(exactly = 1) { GuideBook.giveToPlayer(player) }
    }

    @Test
    fun `repeat join does not give duplicate`() {
        val listener = GuideBookListener(TestHelper.mockPlugin)
        val uuid = UUID.randomUUID()

        val player = mockk<Player>(relaxed = true)
        every { player.uniqueId } returns uuid

        val event = mockk<PlayerJoinEvent>(relaxed = true)
        every { event.player } returns player

        listener.onPlayerJoin(event)
        listener.onPlayerJoin(event)

        verify(exactly = 1) { GuideBook.giveToPlayer(player) }
    }
}

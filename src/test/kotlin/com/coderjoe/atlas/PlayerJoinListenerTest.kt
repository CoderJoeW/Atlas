package com.coderjoe.atlas

import io.mockk.*
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import org.junit.jupiter.api.*

class PlayerJoinListenerTest {

    @BeforeEach
    fun setup() {
        TestHelper.setup()
    }

    @AfterEach
    fun teardown() {
        TestHelper.teardown()
    }

    @Test
    fun `onPlayerJoin sends pack when configured`() {
        val manager = mockk<ResourcePackManager>(relaxed = true)
        every { manager.isConfigured() } returns true

        val listener = PlayerJoinListener(manager)
        val player = mockk<Player>(relaxed = true)
        val event = mockk<PlayerJoinEvent>(relaxed = true)
        every { event.player } returns player

        listener.onPlayerJoin(event)
        verify(exactly = 1) { manager.sendToPlayer(player) }
    }

    @Test
    fun `onPlayerJoin does not send when not configured`() {
        val manager = mockk<ResourcePackManager>(relaxed = true)
        every { manager.isConfigured() } returns false

        val listener = PlayerJoinListener(manager)
        val player = mockk<Player>(relaxed = true)
        val event = mockk<PlayerJoinEvent>(relaxed = true)
        every { event.player } returns player

        listener.onPlayerJoin(event)
        verify(exactly = 0) { manager.sendToPlayer(any()) }
    }
}

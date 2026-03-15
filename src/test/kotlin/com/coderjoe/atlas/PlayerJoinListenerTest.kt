package com.coderjoe.atlas

import io.mockk.every
import io.mockk.mockk
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
    fun `onPlayerJoin does not throw`() {
        val listener = PlayerJoinListener()
        val player = mockk<Player>(relaxed = true)
        every { player.server } returns TestHelper.mockServer
        val event = mockk<PlayerJoinEvent>(relaxed = true)
        every { event.player } returns player

        assertDoesNotThrow { listener.onPlayerJoin(event) }
    }
}

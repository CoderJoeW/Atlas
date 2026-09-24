package com.coderjoe.atlas.listener

import com.coderjoe.atlas.item.GuideBook
import com.coderjoe.atlas.testing.MockServer
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class GuideBookListenerTest {
    @BeforeEach
    fun setup() {
        MockServer.setup()
        mockkObject(GuideBook)
        every { GuideBook.giveToPlayer(any()) } just Runs
    }

    @AfterEach
    fun teardown() {
        unmockkObject(GuideBook)
        MockServer.teardown()
    }

    @Test
    fun `first join gives book`() {
        val listener = GuideBookListener(MockServer.plugin)

        val player = mockk<Player>(relaxed = true)
        every { player.uniqueId } returns UUID.randomUUID()

        val event = mockk<PlayerJoinEvent>(relaxed = true)
        every { event.player } returns player

        listener.onPlayerJoin(event)

        verify(exactly = 1) { GuideBook.giveToPlayer(player) }
    }

    @Test
    fun `repeat join does not give duplicate`() {
        val listener = GuideBookListener(MockServer.plugin)
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

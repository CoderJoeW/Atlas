package com.coderjoe.atlas

import io.mockk.*
import net.kyori.adventure.resource.ResourcePackRequest
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

class ResourcePackManagerTest {

    @BeforeEach
    fun setup() {
        TestHelper.setup()
    }

    @AfterEach
    fun teardown() {
        TestHelper.teardown()
    }

    private fun createManagerWithConfig(enabled: Boolean, url: String = "", hash: String = ""): ResourcePackManager {
        val config = YamlConfiguration()
        config.set("resource-pack.enabled", enabled)
        config.set("resource-pack.url", url)
        config.set("resource-pack.hash", hash)
        config.set("resource-pack.required", false)
        config.set("resource-pack.prompt", "")

        every { TestHelper.mockPlugin.config } returns config

        val manager = ResourcePackManager(TestHelper.mockPlugin)
        manager.load()
        return manager
    }

    @Test
    fun `load disabled - isConfigured false`() {
        val manager = createManagerWithConfig(enabled = false)
        assertFalse(manager.isConfigured())
    }

    @Test
    fun `load blank URL - isConfigured false`() {
        val manager = createManagerWithConfig(enabled = true, url = "")
        assertFalse(manager.isConfigured())
    }

    @Test
    fun `load valid config - isConfigured true`() {
        val manager = createManagerWithConfig(enabled = true, url = "https://example.com/pack.zip", hash = "abc123")
        assertTrue(manager.isConfigured())
    }

    @Test
    fun `sendToPlayer when not configured is no-op`() {
        val manager = createManagerWithConfig(enabled = false)
        val player = mockk<Player>(relaxed = true)

        manager.sendToPlayer(player)
        verify(exactly = 0) { player.sendResourcePacks(any<ResourcePackRequest>()) }
    }

    @Test
    fun `sendToPlayer when configured sends pack`() {
        val manager = createManagerWithConfig(enabled = true, url = "https://example.com/pack.zip", hash = "abc123")
        val player = mockk<Player>(relaxed = true)

        manager.sendToPlayer(player)
        verify(exactly = 1) { player.sendResourcePacks(any<ResourcePackRequest>()) }
    }
}

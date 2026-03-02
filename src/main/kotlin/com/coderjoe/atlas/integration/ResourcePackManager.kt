package com.coderjoe.atlas.integration

import net.kyori.adventure.resource.ResourcePackInfo
import net.kyori.adventure.resource.ResourcePackRequest
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.net.URI
import java.util.UUID

class ResourcePackManager(private val plugin: JavaPlugin) {

    private var packInfo: ResourcePackInfo? = null
    private var isRequired: Boolean = false
    private var promptMessage: Component? = null

    fun load() {
        plugin.saveDefaultConfig()
        val config = plugin.config

        val enabled = config.getBoolean("resource-pack.enabled", false)
        if (!enabled) {
            plugin.logger.info("Resource pack is disabled in config")
            return
        }

        val url = config.getString("resource-pack.url") ?: ""
        val hash = config.getString("resource-pack.hash") ?: ""
        isRequired = config.getBoolean("resource-pack.required", false)
        val prompt = config.getString("resource-pack.prompt") ?: ""

        if (url.isBlank()) {
            plugin.logger.warning("Resource pack URL is not configured!")
            return
        }

        // Generate a consistent UUID based on the URL
        val packId = UUID.nameUUIDFromBytes(url.toByteArray())

        try {
            packInfo = ResourcePackInfo.resourcePackInfo()
                .uri(URI.create(url))
                .hash(hash)
                .id(packId)
                .build()

            if (prompt.isNotBlank()) {
                promptMessage = Component.text(prompt)
            }

            plugin.logger.info("Resource pack configured: $url")
        } catch (e: Exception) {
            plugin.logger.severe("Failed to configure resource pack: ${e.message}")
        }
    }

    fun sendToPlayer(player: Player) {
        val info = packInfo ?: return

        val request = ResourcePackRequest.resourcePackRequest()
            .packs(info)
            .required(isRequired)
            .replace(true)
            .apply {
                promptMessage?.let { prompt(it) }
            }
            .build()

        player.sendResourcePacks(request)

        plugin.logger.info("Sent resource pack to ${player.name}")
    }

    fun isConfigured(): Boolean = packInfo != null
}

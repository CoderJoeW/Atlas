package com.coderjoe.atlas.listener

import com.coderjoe.atlas.integration.ResourcePackManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class PlayerJoinListener(
    private val resourcePackManager: ResourcePackManager
) : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        // Send resource pack to the player
        if (resourcePackManager.isConfigured()) {
            resourcePackManager.sendToPlayer(player)
        }
    }
}

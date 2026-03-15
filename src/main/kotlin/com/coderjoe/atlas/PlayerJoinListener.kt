package com.coderjoe.atlas

import org.bukkit.Bukkit
import org.bukkit.Keyed
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class PlayerJoinListener : Listener {
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val plugin = player.server.pluginManager.getPlugin("Atlas") ?: return
        player.server.scheduler.runTaskLater(plugin, Runnable {
            val iterator = Bukkit.recipeIterator()
            while (iterator.hasNext()) {
                val recipe = iterator.next()
                if (recipe is Keyed) {
                    val key = recipe.key
                    if (key.namespace == "atlas") {
                        player.discoverRecipe(key)
                    }
                }
            }
        }, 20L)
    }
}

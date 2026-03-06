package com.coderjoe.atlas.core

import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object AtlasBlockDialog {
    private lateinit var plugin: JavaPlugin
    private val activeDialogs = ConcurrentHashMap<UUID, BukkitTask>()

    fun init(plugin: JavaPlugin) {
        this.plugin = plugin
    }

    fun cleanup() {
        activeDialogs.values.forEach { it.cancel() }
        activeDialogs.clear()
    }

    fun showDialog(
        player: Player,
        block: AtlasBlock,
        registry: BlockRegistry<*>,
        renderDialog: (Player, AtlasBlock, onClose: (Player) -> Unit) -> Unit
    ) {
        activeDialogs.remove(player.uniqueId)?.cancel()

        val onClose: (Player) -> Unit = { p -> activeDialogs.remove(p.uniqueId)?.cancel() }

        renderDialog(player, block, onClose)

        val task = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            if (!player.isOnline) {
                activeDialogs.remove(player.uniqueId)?.cancel()
                return@Runnable
            }
            if (player.location.distance(block.location) > 10) {
                activeDialogs.remove(player.uniqueId)?.cancel()
                return@Runnable
            }
            if (registry.getBlock(block.location) == null) {
                activeDialogs.remove(player.uniqueId)?.cancel()
                return@Runnable
            }
            renderDialog(player, block, onClose)
        }, 10L, 10L)

        activeDialogs[player.uniqueId] = task
    }
}

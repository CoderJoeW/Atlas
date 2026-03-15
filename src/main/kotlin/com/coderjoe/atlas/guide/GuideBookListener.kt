package com.coderjoe.atlas.guide

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class GuideBookListener(private val plugin: JavaPlugin) : Listener {
    private val recipientsFile = File(plugin.dataFolder, "guide-recipients.yml")
    private val config = YamlConfiguration()
    private val recipients: MutableSet<String>

    init {
        if (recipientsFile.exists()) {
            config.load(recipientsFile)
        }
        recipients = config.getStringList("recipients").toMutableSet()
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val uuid = player.uniqueId.toString()

        if (uuid !in recipients) {
            GuideBook.giveToPlayer(player)
            recipients.add(uuid)
            config.set("recipients", recipients.toList())
            config.save(recipientsFile)
        }
    }
}

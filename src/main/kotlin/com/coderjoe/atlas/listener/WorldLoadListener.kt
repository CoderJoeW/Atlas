package com.coderjoe.atlas.listener

import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.data.BlockPersistence
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.world.WorldLoadEvent

class WorldLoadListener(
    private val persistence: BlockPersistence,
    private val registry: BlockRegistry,
) : Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    fun onWorldLoad(event: WorldLoadEvent) = persistence.restoreWorld(registry, event.world)
}

package com.coderjoe.atlas.listener

import com.coderjoe.atlas.data.ChunkOreSurvey
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent

class ChunkSurveyListener(private val survey: ChunkOreSurvey) : Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    fun onChunkLoad(event: ChunkLoadEvent) = survey.survey(event.chunk)
}

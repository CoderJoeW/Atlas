package com.coderjoe.atlas.core

import com.coderjoe.atlas.Atlas
import com.nexomc.nexo.api.NexoBlocks
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask

abstract class AtlasBlock(
    val location: Location
) {
    private var updateTask: BukkitTask? = null
    protected val plugin: JavaPlugin get() = testPlugin ?: JavaPlugin.getPlugin(Atlas::class.java)
    protected open val updateIntervalTicks: Long = 20L
    private var currentVisualState: String? = null

    companion object {
        @JvmStatic
        internal var testPlugin: JavaPlugin? = null
    }

    protected abstract fun blockUpdate()
    abstract fun getVisualStateBlockId(): String
    abstract fun getRegistry(): BlockRegistry<*>

    open val facing: BlockFace get() = BlockFace.SELF
    open val baseBlockId: String get() = ""

    protected fun updateVisualState() {
        val newState = getVisualStateBlockId()
        if (newState != currentVisualState) {
            val registry = getRegistry()
            val key = BlockRegistry.locationKey(location)
            registry.updatingLocations.add(key)
            try {
                location.block.setType(Material.AIR, false)
                NexoBlocks.place(newState, location)
                currentVisualState = newState
            } finally {
                registry.updatingLocations.remove(key)
            }
        }
    }

    fun start() {
        currentVisualState = NexoBlocks.customBlockMechanic(location.block)?.itemID

        plugin.server.scheduler.runTask(plugin, Runnable {
            updateVisualState()
        })

        updateTask = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            try {
                blockUpdate()
                updateVisualState()
            } catch (e: Exception) {
                plugin.logger.warning("Error in block tick at ${location.blockX},${location.blockY},${location.blockZ}: ${e.message}")
            }
        }, updateIntervalTicks, updateIntervalTicks)

        plugin.logger.info("${this::class.simpleName} at ${location.blockX},${location.blockY},${location.blockZ} started")
    }

    fun stop() {
        updateTask?.cancel()
        updateTask = null
        plugin.logger.info("${this::class.simpleName} at ${location.blockX},${location.blockY},${location.blockZ} stopped")
    }
}

package com.coderjoe.atlas.power

import com.coderjoe.atlas.Atlas
import com.nexomc.nexo.api.NexoBlocks
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask

/**
 * Represents a block that participates in the power network
 */
abstract class PowerBlock(
    val location: Location,
    val maxStorage: Int,
    var currentPower: Int = 0
) {
    private var updateTask: BukkitTask? = null
    protected val plugin: JavaPlugin = JavaPlugin.getPlugin(Atlas::class.java)
    protected open val updateIntervalTicks: Long = 100L
    protected open val canReceivePower: Boolean = true

    fun hasPower(): Boolean = currentPower > 0

    fun canAcceptPower(): Boolean = canReceivePower && currentPower < maxStorage

    fun addPower(amount: Int): Int {
        val spaceAvailable = maxStorage - currentPower
        val toAdd = minOf(amount, spaceAvailable)
        currentPower += toAdd
        return toAdd
    }

    fun removePower(amount: Int): Int {
        val toRemove = minOf(amount, currentPower)
        currentPower -= toRemove
        return toRemove
    }

    protected abstract fun powerUpdate()
    abstract fun getVisualStateBlockId(): String
    private var currentVisualState: String? = null

    protected fun updateVisualState() {
        val newState = getVisualStateBlockId()
        if (newState != currentVisualState) {
            val key = PowerBlockRegistry.locationKey(location)
            val registry = PowerBlockRegistry.instance ?: return
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
        // Snapshot the current block so the deferred update is a no-op when already correct
        currentVisualState = NexoBlocks.customBlockMechanic(location.block)?.itemID

        // Defer to next tick — corrects visual state if it doesn't match (e.g. after persistence load)
        plugin.server.scheduler.runTask(plugin, Runnable {
            updateVisualState()
        })

        updateTask = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            try {
                powerUpdate()
                updateVisualState()
            } catch (e: Exception) {
                plugin.logger.warning("Error in power block tick at ${location.blockX},${location.blockY},${location.blockZ}: ${e.message}")
            }
        }, updateIntervalTicks, updateIntervalTicks)

        plugin.logger.info("${this::class.simpleName} at ${location.blockX},${location.blockY},${location.blockZ} started - updating every ${updateIntervalTicks / 20} seconds")
    }

    fun stop() {
        updateTask?.cancel()
        updateTask = null
        plugin.logger.info("${this::class.simpleName} at ${location.blockX},${location.blockY},${location.blockZ} stopped")
    }
}

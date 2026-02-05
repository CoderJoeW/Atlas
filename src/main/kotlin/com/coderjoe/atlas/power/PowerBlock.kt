package com.coderjoe.atlas.power

import com.coderjoe.atlas.Atlas
import org.bukkit.Location
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

    companion object {
        private const val UPDATE_INTERVAL_TICKS = 100L // 20 seconds (20 ticks per second)
    }

    fun hasPower(): Boolean = currentPower > 0

    fun canAcceptPower(): Boolean = currentPower < maxStorage

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

    fun start() {
        updateTask = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            powerUpdate()
        }, UPDATE_INTERVAL_TICKS, UPDATE_INTERVAL_TICKS)

        plugin.logger.info("${this::class.simpleName} at ${location.blockX},${location.blockY},${location.blockZ} started - updating every 20 seconds")
    }

    fun stop() {
        updateTask?.cancel()
        updateTask = null
        plugin.logger.info("${this::class.simpleName} at ${location.blockX},${location.blockY},${location.blockZ} stopped")
    }
}

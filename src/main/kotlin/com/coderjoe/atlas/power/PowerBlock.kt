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

    fun start() {
        updateTask = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            powerUpdate()
        }, updateIntervalTicks, updateIntervalTicks)

        plugin.logger.info("${this::class.simpleName} at ${location.blockX},${location.blockY},${location.blockZ} started - updating every ${updateIntervalTicks / 20} seconds")
    }

    fun stop() {
        updateTask?.cancel()
        updateTask = null
        plugin.logger.info("${this::class.simpleName} at ${location.blockX},${location.blockY},${location.blockZ} stopped")
    }
}

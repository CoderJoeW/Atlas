package com.coderjoe.atlas.fluid

import com.coderjoe.atlas.Atlas
import com.nexomc.nexo.api.NexoBlocks
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask

abstract class FluidBlock(
    val location: Location,
    var storedFluid: FluidType = FluidType.NONE
) {
    private var updateTask: BukkitTask? = null
    protected val plugin: JavaPlugin = JavaPlugin.getPlugin(Atlas::class.java)
    protected open val updateIntervalTicks: Long = 20L

    fun hasFluid(): Boolean = storedFluid != FluidType.NONE

    fun storeFluid(type: FluidType): Boolean {
        if (storedFluid != FluidType.NONE) return false
        storedFluid = type
        return true
    }

    fun removeFluid(): FluidType {
        val fluid = storedFluid
        storedFluid = FluidType.NONE
        return fluid
    }

    protected abstract fun fluidUpdate()
    abstract fun getVisualStateBlockId(): String
    private var currentVisualState: String? = null

    protected fun updateVisualState() {
        val newState = getVisualStateBlockId()
        if (newState != currentVisualState) {
            val key = FluidBlockRegistry.locationKey(location)
            val registry = FluidBlockRegistry.instance ?: return
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
                fluidUpdate()
                updateVisualState()
            } catch (e: Exception) {
                plugin.logger.warning("Error in fluid block tick at ${location.blockX},${location.blockY},${location.blockZ}: ${e.message}")
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

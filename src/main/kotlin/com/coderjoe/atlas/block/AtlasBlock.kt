package com.coderjoe.atlas.block

import com.coderjoe.atlas.craftengine.CraftEngineHelper
import com.coderjoe.atlas.util.atlasInfo
import com.coderjoe.atlas.util.coordinates
import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask

abstract class AtlasBlock(
    val location: Location,
) {
    private var updateTask: BukkitTask? = null
    private var effectTask: BukkitTask? = null
    private var context: BlockContext? = null
    protected val plugin: JavaPlugin get() = requireContext().plugin
    protected open val updateIntervalTicks: Long = 20L

    /** Tick interval for [spawnEffects]. Zero disables the ambient effect task entirely. */
    protected open val effectIntervalTicks: Long = 0L
    private var currentVisualState: String? = null

    companion object {
        val ADJACENT_FACES =
            listOf(
                BlockFace.NORTH,
                BlockFace.SOUTH,
                BlockFace.EAST,
                BlockFace.WEST,
                BlockFace.UP,
                BlockFace.DOWN,
            )
    }

    open fun writeSaveData(data: MutableMap<String, Any>) {}

    open fun readSaveData(data: Map<String, Any?>) {}

    open fun inspect(): Inspection = Inspection()

    protected abstract fun blockUpdate()

    /** Ambient visuals, run on its own timer at [effectIntervalTicks]. Purely cosmetic. */
    internal open fun spawnEffects() {}

    abstract fun getVisualStateBlockId(): String

    open val facing: BlockFace get() = BlockFace.SELF
    open val baseBlockId: String get() = ""

    protected fun updateVisualState() {
        val newState = getVisualStateBlockId()
        if (newState == currentVisualState) return

        val updating = requireContext().registry.updatingLocations
        val key = BlockRegistry.locationKey(location)
        updating.add(key)

        try {
            CraftEngineHelper.placeState(location, newState)
            currentVisualState = newState
        } catch (e: Throwable) {
            plugin.logger.warning("Failed to update visual state at ${location.coordinates}: ${e.message}")
        } finally {
            updating.remove(key)
        }
    }

    /** The block against [face], whatever family it belongs to, or null when that square holds none. */
    fun neighbor(face: BlockFace): AtlasBlock? = requireContext().registry.getAdjacentBlock(location, face)

    private fun requireContext(): BlockContext {
        return checkNotNull(context) {
            "${this::class.simpleName} at ${location.coordinates} is not registered"
        }
    }

    fun start() {
        currentVisualState = CraftEngineHelper.getBlockId(location.block)

        plugin.server.scheduler.runTask(
            plugin,
            Runnable {
                updateVisualState()
                if (facing != BlockFace.SELF) {
                    CraftEngineHelper.setFacing(location, facing)
                }
            },
        )

        updateTask =
            plugin.server.scheduler.runTaskTimer(
                plugin,
                Runnable {
                    try {
                        blockUpdate()
                        updateVisualState()
                    } catch (e: Exception) {
                        plugin.logger.warning(
                            """
                            Error in block tick at ${location.coordinates}: ${e.message}
                            """.trimIndent(),
                        )
                    }
                },
                updateIntervalTicks, updateIntervalTicks,
            )

        if (effectIntervalTicks > 0) {
            effectTask =
                plugin.server.scheduler.runTaskTimer(
                    plugin,
                    Runnable {
                        try {
                            spawnEffects()
                        } catch (e: Exception) {
                            plugin.logger.warning(
                                """
                                Error in block effects at ${location.coordinates}: ${e.message}
                                """.trimIndent(),
                            )
                        }
                    },
                    effectIntervalTicks, effectIntervalTicks,
                )
        }

        plugin.logger.atlasInfo("${this::class.simpleName} at ${location.coordinates} started")
    }

    fun stop() {
        updateTask?.cancel()
        updateTask = null
        effectTask?.cancel()
        effectTask = null
        plugin.logger.atlasInfo("${this::class.simpleName} at ${location.coordinates} stopped")
    }

    internal fun attach(context: BlockContext) {
        this.context = context
    }
}

package com.coderjoe.atlas.core

import com.coderjoe.atlas.Atlas
import com.coderjoe.atlas.atlasInfo
import com.coderjoe.atlas.coordinates
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks
import net.momirealms.craftengine.core.util.Key
import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask

abstract class AtlasBlock(
    val location: Location,
) {
    private var updateTask: BukkitTask? = null
    private var effectTask: BukkitTask? = null
    protected val plugin: JavaPlugin get() = testPlugin ?: JavaPlugin.getPlugin(Atlas::class.java)
    protected open val updateIntervalTicks: Long = 20L

    /** Tick interval for [spawnEffects]. Zero disables the ambient effect task entirely. */
    protected open val effectIntervalTicks: Long = 0L
    private var currentVisualState: String? = null

    companion object {
        @JvmStatic
        internal var testPlugin: JavaPlugin? = null

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

    protected abstract fun blockUpdate()

    /** Ambient visuals, run on its own timer at [effectIntervalTicks]. Purely cosmetic. */
    protected open fun spawnEffects() {}

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
                CraftEngineBlocks.place(location, Key.of(newState), false)
                currentVisualState = newState
            } catch (e: Throwable) {
                plugin.logger.warning(
                    "Failed to update visual state at ${location.coordinates}: ${e.message}",
                )
            } finally {
                registry.updatingLocations.remove(key)
            }
        }
    }

    fun start() {
        try {
            val state = CraftEngineBlocks.getCustomBlockState(location.block)
            currentVisualState = state?.owner()?.value()?.id()?.toString()
        } catch (_: Throwable) {
            // CraftEngine not loaded
        }

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
}

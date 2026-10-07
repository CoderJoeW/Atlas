package com.coderjoe.atlas.block

import com.coderjoe.atlas.block.deposit.DepositMap
import com.coderjoe.atlas.craftengine.CraftEngineHelper
import com.coderjoe.atlas.util.atlasInfo
import com.coderjoe.atlas.util.coordinates
import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.bukkit.plugin.java.JavaPlugin

abstract class AtlasBlock(
    val location: Location,
) {
    private var context: BlockContext? = null
    protected val plugin: JavaPlugin get() = requireContext().plugin
    protected val deposits: DepositMap get() = requireContext().deposits
    protected val registry: BlockRegistry get() = requireContext().registry
    internal open val updateIntervalTicks: Long = 20L

    /** Tick interval for [spawnEffects]. Zero leaves the block out of the effect lane entirely. */
    internal open val effectIntervalTicks: Long = 0L

    internal val isAttached: Boolean get() = context != null
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

    /** Ambient visuals, run by the ticker every [effectIntervalTicks]. Purely cosmetic. */
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

        requireContext().registry.ticker.schedule(this)
        plugin.logger.atlasInfo("${this::class.simpleName} at ${location.coordinates} started")
    }

    fun stop() {
        val ticker = requireContext().registry.ticker
        if (!ticker.isScheduled(this)) return
        ticker.unschedule(this)
        plugin.logger.atlasInfo("${this::class.simpleName} at ${location.coordinates} stopped")
    }

    internal fun runUpdate() {
        blockUpdate()
        updateVisualState()
    }

    internal fun runEffects() {
        spawnEffects()
    }

    /** Called by the registry once this block is indexed and can see its neighbours. */
    internal open fun onPlaced() {}

    /** Called by the registry once this block is gone from the index. */
    internal open fun onRemoved() {}

    /** Called by the registry when a block next to this one is placed or removed. */
    internal open fun onNeighborChanged() {}

    internal fun attach(context: BlockContext) {
        this.context = context
    }
}

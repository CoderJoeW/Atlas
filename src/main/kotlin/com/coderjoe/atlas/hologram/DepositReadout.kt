package com.coderjoe.atlas.hologram

import com.coderjoe.atlas.block.deposit.Ore
import com.coderjoe.atlas.block.deposit.Purity
import com.coderjoe.atlas.block.tone
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Chunk
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask

/**
 * The goggles' survey readout: the ore deposits in the chunk a wearer is standing in, on their
 * action bar, so a site can be scouted before a mine goes down on it.
 */
class DepositReadout(
    private val plugin: JavaPlugin,
    private val oreIn: (Chunk) -> Map<Ore, Int>?,
    private val isWearing: (Player) -> Boolean,
) {
    private var task: BukkitTask? = null

    fun start() {
        task?.cancel()
        task = plugin.server.scheduler.runTaskTimer(plugin, Runnable { refresh() }, REFRESH_TICKS, REFRESH_TICKS)
    }

    fun stop() {
        task?.cancel()
        task = null
    }

    internal fun refresh() {
        plugin.server.onlinePlayers.filter(isWearing).forEach { it.sendActionBar(readout(oreIn(it.chunk))) }
    }

    internal companion object {
        /** Well inside the action bar's fade-out, so the line holds steady while the goggles stay on. */
        private const val REFRESH_TICKS = 20L

        internal fun readout(ore: Map<Ore, Int>?): Component {
            if (ore == null) return Component.text("Surveying this chunk...").color(NamedTextColor.GRAY)

            val deposits =
                Ore.entries
                    .map { it to it.purityOf(ore.getValue(it)) }
                    .filter { (_, purity) -> purity != Purity.BARREN }
            if (deposits.isEmpty()) return Component.text("No ore deposits in this chunk").color(NamedTextColor.GRAY)

            val parts =
                deposits.map { (kind, purity) ->
                    Component.text("${kind.displayName} ${purity.displayName}").color(purity.tone.color)
                }
            val separator = JoinConfiguration.separator(Component.text(" · ").color(NamedTextColor.DARK_GRAY))
            return Component.text("Deposits: ").color(NamedTextColor.GRAY).append(Component.join(separator, parts))
        }
    }
}

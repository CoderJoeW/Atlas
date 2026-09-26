package com.coderjoe.atlas.hologram

import com.coderjoe.atlas.block.deposit.Ore
import com.coderjoe.atlas.block.deposit.Purity
import com.coderjoe.atlas.block.tone
import io.papermc.paper.scoreboard.numbers.NumberFormat
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Chunk
import org.bukkit.World.Environment
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard
import java.util.UUID

/**
 * The goggles' survey readout: the purity of every ore in the chunk a wearer is standing in, stacked
 * down the sidebar on the right of the screen, so a site can be scouted before a mine goes down on it.
 *
 * A wearer is given a scoreboard of their own while the goggles are on, and handed back the one they
 * had when they come off.
 */
class DepositReadout(
    private val plugin: JavaPlugin,
    private val oreIn: (Chunk) -> Map<Ore, Int>?,
    private val isWearing: (Player) -> Boolean,
) {
    private var task: BukkitTask? = null
    private val sidebars = mutableMapOf<UUID, Sidebar>()

    fun start() {
        task?.cancel()
        task = plugin.server.scheduler.runTaskTimer(plugin, Runnable { refresh() }, REFRESH_TICKS, REFRESH_TICKS)
    }

    fun stop() {
        task?.cancel()
        task = null
        plugin.server.onlinePlayers.forEach(::takeSidebar)
        sidebars.clear()
    }

    internal fun refresh() {
        val online = plugin.server.onlinePlayers
        sidebars.keys.retainAll(online.map { it.uniqueId }.toSet())
        online.forEach { player ->
            if (isWearing(player)) {
                sidebarOf(player).show(readout(oreIn(player.chunk), player.world.environment))
            } else {
                takeSidebar(player)
            }
        }
    }

    private fun sidebarOf(player: Player): Sidebar =
        sidebars.getOrPut(player.uniqueId) {
            Sidebar(player.scoreboard, plugin.server.scoreboardManager.newScoreboard).also { player.scoreboard = it.board }
        }

    private fun takeSidebar(player: Player) {
        val sidebar = sidebars.remove(player.uniqueId) ?: return
        if (player.scoreboard === sidebar.board) player.scoreboard = sidebar.previous
    }

    /** One line of the readout: [label] on the left of the sidebar, [value] right-aligned beside it. */
    internal data class Line(val label: Component, val value: Component = Component.empty())

    private class Sidebar(val previous: Scoreboard, val board: Scoreboard) {
        private val objective: Objective =
            board.registerNewObjective(OBJECTIVE, Criteria.DUMMY, TITLE).apply {
                displaySlot = DisplaySlot.SIDEBAR
                numberFormat(NumberFormat.blank())
            }
        private var shown: List<Line> = emptyList()

        fun show(lines: List<Line>) {
            if (lines == shown) return
            (lines.size until shown.size).forEach { board.resetScores(entry(it)) }
            lines.forEachIndexed { index, line ->
                objective.getScore(entry(index)).apply {
                    score = lines.size - index
                    customName(line.label)
                    numberFormat(NumberFormat.fixed(line.value))
                }
            }
            shown = lines
        }

        private fun entry(index: Int) = "line$index"
    }

    internal companion object {
        /** Scores stay put until changed, so this only sets how soon a step into a new chunk shows. */
        private const val REFRESH_TICKS = 20L

        private const val OBJECTIVE = "atlas_deposits"
        private val TITLE = Component.text("Chunk Deposits").color(NamedTextColor.GOLD)

        internal fun readout(
            ore: Map<Ore, Int>?,
            dimension: Environment,
        ): List<Line> {
            if (ore == null) return listOf(Line(Component.text("Surveying...").color(NamedTextColor.GRAY)))

            val native = Ore.entries.filter { it.dimension == dimension }
            if (native.isEmpty()) return listOf(Line(Component.text("No ore here").color(NamedTextColor.GRAY)))

            return native.map { kind ->
                val purity = kind.purityOf(ore.getValue(kind))
                val color = if (purity == Purity.BARREN) NamedTextColor.DARK_GRAY else purity.tone.color
                Line(Component.text(kind.displayName).color(NamedTextColor.GRAY), Component.text(purity.displayName).color(color))
            }
        }
    }
}

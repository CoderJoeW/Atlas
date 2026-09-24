package com.coderjoe.atlas.dialog

import com.coderjoe.atlas.block.AtlasBlock
import com.coderjoe.atlas.block.BlockCatalog
import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.block.Gauge
import com.coderjoe.atlas.block.StatusLine
import com.coderjoe.atlas.block.Tone
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * The wrench's readout: a notice dialog for one block, redrawn every half second while the
 * player stays near it and the block still exists.
 *
 * Built once in `onEnable` with what it needs, so nothing has to be initialized ahead of it, and
 * [cleanup] cancels every refresh still running when the plugin shuts down.
 */
class BlockInspectorDialog(
    private val plugin: JavaPlugin,
    private val registry: BlockRegistry,
    private val catalog: BlockCatalog,
) {
    private val activeDialogs = ConcurrentHashMap<UUID, BukkitTask>()

    fun show(
        player: Player,
        block: AtlasBlock,
    ) {
        activeDialogs.remove(player.uniqueId)?.cancel()

        val onClose: (Player) -> Unit = { p -> activeDialogs.remove(p.uniqueId)?.cancel() }

        render(player, block, onClose)

        val task =
            plugin.server.scheduler.runTaskTimer(
                plugin,
                Runnable {
                    if (!player.isOnline ||
                        player.location.distance(block.location) > MAX_DISTANCE ||
                        registry.getBlock(block.location) == null
                    ) {
                        activeDialogs.remove(player.uniqueId)?.cancel()
                        return@Runnable
                    }
                    render(player, block, onClose)
                },
                REFRESH_TICKS,
                REFRESH_TICKS,
            )

        activeDialogs[player.uniqueId] = task
    }

    fun cleanup() {
        activeDialogs.values.forEach { it.cancel() }
        activeDialogs.clear()
    }

    private fun render(
        viewer: Player,
        block: AtlasBlock,
        onClose: (Player) -> Unit,
    ) {
        val descriptor = catalog.find(block.baseBlockId)
        val title =
            Component.text(
                AtlasBlockDialog.defaultDisplayName(descriptor, block.facing, fallback = "Atlas Block"),
            )
        viewer.showDialog(
            AtlasBlockDialog.createNoticeDialog(title, body(block, descriptor?.description), onClose),
        )
    }

    internal companion object {
        private const val BAR_LENGTH = 10
        private const val MAX_DISTANCE = 10.0
        private const val REFRESH_TICKS = 10L

        internal fun body(
            block: AtlasBlock,
            description: String?,
        ): Component {
            val inspection = block.inspect()
            val parts = mutableListOf<Component>()
            inspection.gauges.forEach { parts += gauge(it) }
            inspection.lines.forEach { parts += line(it) }
            if (description != null) parts += Component.text(description).color(NamedTextColor.GRAY)

            var body = Component.empty()
            parts.forEachIndexed { index, part ->
                if (index > 0) body = body.append(Component.newline())
                body = body.append(part)
            }
            return body
        }

        private fun gauge(gauge: Gauge): Component {
            val ratio = if (gauge.max > 0) gauge.current.toFloat() / gauge.max else 0f
            val filled = (ratio * BAR_LENGTH).toInt()
            val barColor =
                when {
                    ratio >= 0.7f -> NamedTextColor.GREEN
                    ratio >= 0.3f -> NamedTextColor.YELLOW
                    else -> NamedTextColor.RED
                }

            val bar =
                Component.text("[")
                    .color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.BOLD, false)
                    .append(Component.text("█".repeat(filled)).color(barColor))
                    .append(Component.text("░".repeat(BAR_LENGTH - filled)).color(NamedTextColor.DARK_GRAY))
                    .append(Component.text("]").color(NamedTextColor.GRAY))
                    .append(Component.text(" ${(ratio * 100).toInt()}%").color(barColor))

            return Component.text("${gauge.label}: ${gauge.current}/${gauge.max}")
                .color(NamedTextColor.WHITE)
                .decorate(TextDecoration.BOLD)
                .append(Component.newline())
                .append(bar)
        }

        private fun line(line: StatusLine): Component =
            Component.text(line.text).color(
                when (line.tone) {
                    Tone.NEUTRAL -> NamedTextColor.GRAY
                    Tone.GOOD -> NamedTextColor.GREEN
                    Tone.WARNING -> NamedTextColor.YELLOW
                    Tone.FAULT -> NamedTextColor.RED
                },
            )
    }
}

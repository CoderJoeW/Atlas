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

object BlockInspectorDialog {
    private const val BAR_LENGTH = 10

    fun show(
        player: Player,
        block: AtlasBlock,
        registry: BlockRegistry,
        catalog: BlockCatalog,
    ) {
        AtlasBlockDialog.showDialog(player, block, registry) { viewer, live, onClose ->
            val descriptor = catalog.find(live.baseBlockId)
            val title =
                Component.text(
                    AtlasBlockDialog.defaultDisplayName(descriptor, live.facing, fallback = "Atlas Block"),
                )
            viewer.showDialog(
                AtlasBlockDialog.createNoticeDialog(title, body(live, descriptor?.description), onClose),
            )
        }
    }

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

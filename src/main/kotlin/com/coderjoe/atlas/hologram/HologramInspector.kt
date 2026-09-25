package com.coderjoe.atlas.hologram

import com.coderjoe.atlas.block.AtlasBlock
import com.coderjoe.atlas.block.BlockCatalog
import com.coderjoe.atlas.block.BlockDescriptor
import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.block.Gauge
import com.coderjoe.atlas.block.StatusLine
import com.coderjoe.atlas.block.Tone
import com.coderjoe.atlas.util.displayName
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Transformation
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.UUID
import kotlin.math.abs

/**
 * The goggles' readout: a text panel floating beside the Atlas block a wearer is looking at.
 *
 * Each wearer gets one [TextDisplay] of their own, spawned hidden from everyone and then shown to
 * that player alone, so two players reading two machines never see each other's panels. It is
 * never saved with the world, and it is removed the moment the wearer looks away, takes the
 * goggles off, leaves, or the plugin stops.
 *
 * [isWearing] decides who gets a panel; the plugin passes the goggles check in, which keeps this
 * class free of the item and lets a test say who is wearing them.
 */
class HologramInspector(
    private val plugin: JavaPlugin,
    private val registry: BlockRegistry,
    private val catalog: BlockCatalog,
    private val isWearing: (Player) -> Boolean,
) {
    private class Panel(val display: TextDisplay, var anchor: Location, var text: Component)

    /** Touched only from the main thread, by the refresh task and by [stop]. */
    private val panels = HashMap<UUID, Panel>()
    private var task: BukkitTask? = null

    fun start() {
        task?.cancel()
        task = plugin.server.scheduler.runTaskTimer(plugin, Runnable { refresh() }, REFRESH_TICKS, REFRESH_TICKS)
    }

    fun stop() {
        task?.cancel()
        task = null
        panels.values.forEach { it.display.remove() }
        panels.clear()
    }

    internal fun refresh() {
        // a player who has left takes their panel with them
        panels.keys.filter { plugin.server.getPlayer(it) == null }.forEach { hide(it) }
        plugin.server.onlinePlayers.forEach { refresh(it) }
    }

    private fun refresh(player: Player) {
        val block = if (isWearing(player)) targetOf(player) else null
        if (block == null) {
            hide(player.uniqueId)
            return
        }

        val anchor = anchor(block.location, player.location)
        val text = panelText(block, catalog.find(block.baseBlockId))
        val panel = panels[player.uniqueId]

        if (panel == null || !panel.display.isValid || panel.display.world != anchor.world) {
            hide(player.uniqueId)
            panels[player.uniqueId] = Panel(spawn(player, anchor, text), anchor, text)
            return
        }

        // only send what changed, so a panel on an idle machine costs no packets at all
        if (panel.anchor != anchor) {
            panel.display.teleport(anchor)
            panel.anchor = anchor
        }
        if (panel.text != text) {
            panel.display.text(text)
            panel.text = text
        }
    }

    private fun targetOf(player: Player): AtlasBlock? {
        val hit = player.getTargetBlockExact(RANGE) ?: return null
        return registry.getBlock(hit.location)
    }

    private fun hide(playerId: UUID) {
        panels.remove(playerId)?.display?.remove()
    }

    private fun spawn(
        viewer: Player,
        anchor: Location,
        text: Component,
    ): TextDisplay {
        val display =
            anchor.world.spawn(anchor, TextDisplay::class.java) { panel ->
                // set before the spawn packet goes out, so no other player ever receives it
                panel.isVisibleByDefault = false
                panel.isPersistent = false
                panel.billboard = Display.Billboard.CENTER
                panel.brightness = Display.Brightness(FULL_BRIGHT, FULL_BRIGHT)
                panel.backgroundColor = BACKGROUND
                panel.isShadowed = true
                panel.teleportDuration = GLIDE_TICKS
                panel.transformation =
                    Transformation(Vector3f(), Quaternionf(), Vector3f(SCALE, SCALE, SCALE), Quaternionf())
                panel.text(text)
            }
        viewer.showEntity(plugin, display)
        return display
    }

    internal companion object {
        /** How far away a machine can be read from - a little past arm's reach. */
        private const val RANGE = 8
        private const val REFRESH_TICKS = 5L

        /** How far in front of the block's face the panel stands, and how high above its base. */
        private const val STANDOFF = 0.75
        private const val HEIGHT = 0.9

        private const val SCALE = 0.6f
        private const val GLIDE_TICKS = 3
        private const val FULL_BRIGHT = 15
        private val BACKGROUND = Color.fromARGB(0xB0, 0x12, 0x14, 0x18)

        private const val BAR_LENGTH = 10

        /**
         * Where the panel floats: just off the side of [block] that faces the viewer, snapped to
         * one of the four sides so it holds still while the viewer shifts about.
         *
         * Standing in front of the face rather than on top of the block keeps it clear of models
         * that overhang their cell, like the mines' gantries.
         */
        internal fun anchor(
            block: Location,
            viewer: Location,
        ): Location {
            val face = sideFacing(block, viewer)
            return Location(
                block.world,
                block.blockX + 0.5 + face.modX * STANDOFF,
                block.blockY + HEIGHT,
                block.blockZ + 0.5 + face.modZ * STANDOFF,
            )
        }

        private fun sideFacing(
            block: Location,
            viewer: Location,
        ): BlockFace {
            val dx = viewer.x - (block.blockX + 0.5)
            val dz = viewer.z - (block.blockZ + 0.5)
            return when {
                abs(dx) >= abs(dz) -> if (dx >= 0) BlockFace.EAST else BlockFace.WEST
                else -> if (dz >= 0) BlockFace.SOUTH else BlockFace.NORTH
            }
        }

        internal fun title(
            descriptor: BlockDescriptor?,
            facing: BlockFace,
        ): String {
            val baseName = descriptor?.displayName ?: "Atlas Block"
            return if (descriptor?.showFacingInDisplayName == true) "$baseName (${facing.displayName()})" else baseName
        }

        internal fun panelText(
            block: AtlasBlock,
            descriptor: BlockDescriptor?,
        ): Component {
            val inspection = block.inspect()
            val parts =
                buildList {
                    add(Component.text(title(descriptor, block.facing)).color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD))
                    inspection.gauges.forEach { add(gauge(it)) }
                    inspection.lines.forEach { add(line(it)) }
                    descriptor?.description?.let { add(Component.text(it).color(NamedTextColor.GRAY)) }
                }
            return Component.join(JoinConfiguration.newlines(), parts)
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
                    .append(Component.text("█".repeat(filled)).color(barColor))
                    .append(Component.text("░".repeat(BAR_LENGTH - filled)).color(NamedTextColor.DARK_GRAY))
                    .append(Component.text("]").color(NamedTextColor.GRAY))
                    .append(Component.text(" ${(ratio * 100).toInt()}%").color(barColor))

            return Component.text("${gauge.label}: ${gauge.current}/${gauge.max}")
                .color(NamedTextColor.WHITE)
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

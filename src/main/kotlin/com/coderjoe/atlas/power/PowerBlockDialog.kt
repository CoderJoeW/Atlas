package com.coderjoe.atlas.power

import com.coderjoe.atlas.core.AtlasBlockDialog
import com.coderjoe.atlas.core.BlockRegistry
import com.coderjoe.atlas.power.block.LavaGenerator
import com.coderjoe.atlas.power.block.PowerCable
import com.coderjoe.atlas.power.block.SmallBattery
import com.coderjoe.atlas.power.block.SmallDrill
import com.coderjoe.atlas.power.block.SmallSolarPanel
import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickCallback
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

object PowerBlockDialog {

    fun init(plugin: JavaPlugin) {
        AtlasBlockDialog.init(plugin)
    }

    fun showPowerDialog(player: Player, powerBlock: PowerBlock, registry: BlockRegistry<*>) {
        AtlasBlockDialog.showDialog(player, powerBlock, registry) { p, block, onClose ->
            sendDialog(p, block as PowerBlock, onClose)
        }
    }

    fun cleanup() {
        AtlasBlockDialog.cleanup()
    }

    private fun sendDialog(player: Player, powerBlock: PowerBlock, onClose: (Player) -> Unit) {
        if (powerBlock is SmallDrill) {
            sendDrillDialog(player, powerBlock, onClose)
        } else {
            sendDefaultDialog(player, powerBlock, onClose)
        }
    }

    private fun sendDefaultDialog(player: Player, powerBlock: PowerBlock, onClose: (Player) -> Unit) {
        val title = Component.text(getBlockDisplayName(powerBlock))
        val bodyText = buildPowerInfo(powerBlock)
        val body = DialogBody.plainMessage(bodyText)

        val closeAction = DialogAction.customClick(
            DialogActionCallback { _, audience ->
                val p = audience as? Player ?: return@DialogActionCallback
                onClose(p)
            },
            ClickCallback.Options.builder().build()
        )

        val closeButton = ActionButton.builder(Component.text("Close"))
            .action(closeAction)
            .build()

        val dialog = Dialog.create { factory ->
            factory.empty()
                .base(
                    DialogBase.builder(title)
                        .body(listOf(body))
                        .canCloseWithEscape(false)
                        .afterAction(DialogBase.DialogAfterAction.CLOSE)
                        .build()
                )
                .type(DialogType.notice(closeButton))
        }

        player.showDialog(dialog)
    }

    private fun sendDrillDialog(player: Player, drill: SmallDrill, onClose: (Player) -> Unit) {
        val title = Component.text("Small Drill")
        val bodyText = buildPowerInfo(drill)
        val body = DialogBody.plainMessage(bodyText)

        val toggleLabel = if (drill.enabled) "Turn Off" else "Turn On"
        val toggleAction = DialogAction.customClick(
            DialogActionCallback { _, _ ->
                drill.toggleEnabled()
            },
            ClickCallback.Options.builder().build()
        )
        val toggleButton = ActionButton.builder(Component.text(toggleLabel))
            .action(toggleAction)
            .build()

        val closeAction = DialogAction.customClick(
            DialogActionCallback { _, audience ->
                val p = audience as? Player ?: return@DialogActionCallback
                onClose(p)
            },
            ClickCallback.Options.builder().build()
        )
        val closeButton = ActionButton.builder(Component.text("Close"))
            .action(closeAction)
            .build()

        val dialog = Dialog.create { factory ->
            factory.empty()
                .base(
                    DialogBase.builder(title)
                        .body(listOf(body))
                        .canCloseWithEscape(false)
                        .afterAction(DialogBase.DialogAfterAction.CLOSE)
                        .build()
                )
                .type(DialogType.confirmation(toggleButton, closeButton))
        }

        player.showDialog(dialog)
    }

    private fun getBlockDisplayName(powerBlock: PowerBlock): String = when (powerBlock) {
        is SmallSolarPanel -> "Small Solar Panel"
        is SmallBattery -> "Small Battery"
        is SmallDrill -> "Small Drill"
        is PowerCable -> "Power Cable (${powerBlock.facing.name.lowercase().replaceFirstChar { it.uppercase() }})"
        is LavaGenerator -> "Lava Generator"
        else -> "Power Block"
    }

    private fun buildPowerInfo(powerBlock: PowerBlock): Component {
        val ratio = if (powerBlock.maxStorage > 0)
            powerBlock.currentPower.toFloat() / powerBlock.maxStorage
        else 0f

        val barLength = 10
        val filled = (ratio * barLength).toInt()
        val empty = barLength - filled

        val barColor = when {
            ratio >= 0.7f -> NamedTextColor.GREEN
            ratio >= 0.3f -> NamedTextColor.YELLOW
            else -> NamedTextColor.RED
        }

        val bar = Component.text("[")
            .color(NamedTextColor.GRAY)
            .append(Component.text("\u2588".repeat(filled)).color(barColor))
            .append(Component.text("\u2591".repeat(empty)).color(NamedTextColor.DARK_GRAY))
            .append(Component.text("]").color(NamedTextColor.GRAY))
            .append(Component.text(" ${(ratio * 100).toInt()}%").color(barColor))

        val powerLine = Component.text("Power: ${powerBlock.currentPower}/${powerBlock.maxStorage}")
            .color(NamedTextColor.WHITE)
            .decorate(TextDecoration.BOLD)

        val infoLine = when (powerBlock) {
            is SmallSolarPanel -> Component.text("Generator - produces 1 power/min during daytime")
                .color(NamedTextColor.GRAY)
            is SmallBattery -> Component.text("Storage - holds up to 10 power")
                .color(NamedTextColor.GRAY)
            is SmallDrill -> {
                val directionName = powerBlock.miningDirection.name.lowercase().replaceFirstChar { it.uppercase() }
                val status = if (powerBlock.enabled) "ON" else "OFF"
                Component.text("Machine - mining $directionName, $status, consumes 10 power/s")
                    .color(NamedTextColor.GRAY)
            }
            is PowerCable -> Component.text("Cable - transfers power in facing direction")
                .color(NamedTextColor.GRAY)
            is LavaGenerator -> Component.text("Generator - produces ${LavaGenerator.POWER_PER_LAVA} power per lava unit")
                .color(NamedTextColor.GRAY)
            else -> Component.text("Power block")
                .color(NamedTextColor.GRAY)
        }

        return powerLine
            .append(Component.newline())
            .append(bar)
            .append(Component.newline())
            .append(infoLine)
    }
}

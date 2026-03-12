package com.coderjoe.atlas.guide

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapelessRecipe
import org.bukkit.inventory.meta.BookMeta
import org.bukkit.plugin.java.JavaPlugin

object GuideBook {

    private const val SOLAR_PANEL = "\uE100"
    private const val LAVA_GENERATOR = "\uE101"
    private const val POWER_CABLE = "\uE102"
    private const val SMALL_BATTERY = "\uE103"
    private const val SMALL_DRILL = "\uE104"
    private const val FLUID_PUMP = "\uE105"
    private const val FLUID_PIPE = "\uE106"
    private const val FLUID_CONTAINER = "\uE107"
    private const val CONVEYOR_BELT = "\uE108"
    private const val AUTO_SMELTER = "\uE109"

    fun create(): ItemStack {
        val book = ItemStack(Material.WRITTEN_BOOK)
        val meta = book.itemMeta as BookMeta

        meta.title(Component.text("Atlas Guide"))
        meta.author(Component.text("Atlas"))

        val pages = buildPages()
        for (page in pages) {
            meta.addPages(page)
        }

        book.itemMeta = meta
        return book
    }

    fun giveToPlayer(player: Player) {
        val book = create()
        if (player.inventory.firstEmpty() != -1) {
            player.inventory.addItem(book)
        } else {
            player.world.dropItem(player.location, book)
            player.sendMessage(
                Component.text("Your inventory was full! The Atlas Guide was dropped at your feet.")
                    .color(NamedTextColor.YELLOW)
            )
        }
    }

    fun createRecipe(plugin: JavaPlugin): ShapelessRecipe {
        val key = NamespacedKey(plugin, "atlas_guide")
        val recipe = ShapelessRecipe(key, create())
        recipe.addIngredient(Material.BOOK)
        return recipe
    }

    internal fun buildPages(): List<Component> {
        val bold = Style.style(TextDecoration.BOLD)
        val darkGray = NamedTextColor.DARK_GRAY
        val darkBlue = NamedTextColor.DARK_BLUE
        val darkGreen = NamedTextColor.DARK_GREEN
        val darkAqua = NamedTextColor.DARK_AQUA
        val darkRed = NamedTextColor.DARK_RED
        val gold = NamedTextColor.GOLD

        return listOf(
            // Page 1: Title
            Component.text()
                .append(Component.text("\n\n"))
                .append(Component.text("Atlas Guide", Style.style(TextDecoration.BOLD).color(darkBlue)))
                .append(Component.text("\n\n"))
                .append(Component.text("A complete guide to\nAtlas machines and\nsystems.\n\n", darkGray))
                .append(Component.text("Systems:\n", bold))
                .append(Component.text(" - Power\n", gold))
                .append(Component.text(" - Fluid\n", darkAqua))
                .append(Component.text(" - Transport", darkGreen))
                .build(),

            // Page 2: Power System overview
            Component.text()
                .append(Component.text("Power System\n", Style.style(TextDecoration.BOLD).color(gold)))
                .append(Component.text("\nGenerators produce\npower. Cables transfer\nit. Batteries store it.\nMachines consume it.\n\n", darkGray))
                .append(Component.text("Pull-based: ", bold))
                .append(Component.text("each block\npulls power from the\nblock behind it\n(opposite its facing\ndirection).", darkGray))
                .build(),

            // Page 3: Small Solar Panel
            Component.text()
                .append(Component.text(SOLAR_PANEL))
                .append(Component.text(" "))
                .append(Component.text("Small Solar Panel\n", Style.style(TextDecoration.BOLD).color(gold)))
                .append(Component.text("\nGenerates 1 power per\nminute during daytime.\n\n", darkGray))
                .append(Component.text("Storage: ", bold))
                .append(Component.text("1\n", darkGray))
                .append(Component.text("Requires: ", bold))
                .append(Component.text("clear sky access\nabove the panel.", darkGray))
                .build(),

            // Page 4: Lava Generator
            Component.text()
                .append(Component.text(LAVA_GENERATOR))
                .append(Component.text(" "))
                .append(Component.text("Lava Generator\n", Style.style(TextDecoration.BOLD).color(gold)))
                .append(Component.text("\nGenerates 5 power per\nlava unit consumed.\n\n", darkGray))
                .append(Component.text("Storage: ", bold))
                .append(Component.text("50\n", darkGray))
                .append(Component.text("Input: ", bold))
                .append(Component.text("pulls lava from\nadjacent fluid blocks.", darkGray))
                .build(),

            // Page 5: Power Cable
            Component.text()
                .append(Component.text(POWER_CABLE))
                .append(Component.text(" "))
                .append(Component.text("Power Cable\n", Style.style(TextDecoration.BOLD).color(gold)))
                .append(Component.text("\nTransfers power in one\ndirection. Pulls from\nthe block behind it.\n\n", darkGray))
                .append(Component.text("Storage: ", bold))
                .append(Component.text("1\n", darkGray))
                .append(Component.text("Tip: ", bold))
                .append(Component.text("place cables in a\nline from generator\nto machine.", darkGray))
                .build(),

            // Page 6: Small Battery
            Component.text()
                .append(Component.text(SMALL_BATTERY))
                .append(Component.text(" "))
                .append(Component.text("Small Battery\n", Style.style(TextDecoration.BOLD).color(gold)))
                .append(Component.text("\nStores power for later\nuse. Visual indicator\nshows charge level.\n\n", darkGray))
                .append(Component.text("Storage: ", bold))
                .append(Component.text("10\n", darkGray))
                .append(Component.text("Tip: ", bold))
                .append(Component.text("place between a\ngenerator and machine\nto buffer power.", darkGray))
                .build(),

            // Page 7: Small Drill
            Component.text()
                .append(Component.text(SMALL_DRILL))
                .append(Component.text(" "))
                .append(Component.text("Small Drill\n", Style.style(TextDecoration.BOLD).color(gold)))
                .append(Component.text("\nMines blocks in its\nfacing direction.\nToggle on/off by\nright-clicking.\n\n", darkGray))
                .append(Component.text("Power cost: ", bold))
                .append(Component.text("10 per block\n", darkGray))
                .append(Component.text("Tip: ", bold))
                .append(Component.text("combine with a\nconveyor belt to auto-\ncollect drops.", darkGray))
                .build(),

            // Page 8: Fluid System overview
            Component.text()
                .append(Component.text("Fluid System\n", Style.style(TextDecoration.BOLD).color(darkAqua)))
                .append(Component.text("\nPumps extract fluid.\nPipes transport it.\nContainers store it.\n\n", darkGray))
                .append(Component.text("Supported fluids:\n", bold))
                .append(Component.text(" - Water\n - Lava\n\n", darkGray))
                .append(Component.text("Pull-based: ", bold))
                .append(Component.text("same as\npower — each block\npulls from behind.", darkGray))
                .build(),

            // Page 9: Fluid Pump
            Component.text()
                .append(Component.text(FLUID_PUMP))
                .append(Component.text(" "))
                .append(Component.text("Fluid Pump\n", Style.style(TextDecoration.BOLD).color(darkAqua)))
                .append(Component.text("\nExtracts fluid from\nadjacent cauldrons or\nsource blocks.\n\n", darkGray))
                .append(Component.text("Power cost: ", bold))
                .append(Component.text("1 per operation\n", darkGray))
                .append(Component.text("Storage: ", bold))
                .append(Component.text("1 unit", darkGray))
                .build(),

            // Page 10: Fluid Pipe
            Component.text()
                .append(Component.text(FLUID_PIPE))
                .append(Component.text(" "))
                .append(Component.text("Fluid Pipe\n", Style.style(TextDecoration.BOLD).color(darkAqua)))
                .append(Component.text("\nTransports fluid in\none direction. Pulls\nfrom the block behind\nit.\n\n", darkGray))
                .append(Component.text("Storage: ", bold))
                .append(Component.text("1 unit\n", darkGray))
                .append(Component.text("Tip: ", bold))
                .append(Component.text("chain pipes from\npump to container.", darkGray))
                .build(),

            // Page 11: Fluid Container
            Component.text()
                .append(Component.text(FLUID_CONTAINER))
                .append(Component.text(" "))
                .append(Component.text("Fluid Container\n", Style.style(TextDecoration.BOLD).color(darkAqua)))
                .append(Component.text("\nStores fluid. Visual\nindicator shows fill\nlevel and fluid type.\n\n", darkGray))
                .append(Component.text("Storage: ", bold))
                .append(Component.text("10 units\n", darkGray))
                .append(Component.text("Tip: ", bold))
                .append(Component.text("store lava for\nuse with the Lava\nGenerator.", darkGray))
                .build(),

            // Page 12: Transport System + Conveyor Belt
            Component.text()
                .append(Component.text("Transport System\n", Style.style(TextDecoration.BOLD).color(darkGreen)))
                .append(Component.text("\n"))
                .append(Component.text(CONVEYOR_BELT))
                .append(Component.text(" "))
                .append(Component.text("Conveyor Belt\n", Style.style(TextDecoration.BOLD).color(darkGreen)))
                .append(Component.text("\nMoves dropped items\nin its facing direction.\nNo power required.\n\n", darkGray))
                .append(Component.text("Tip: ", bold))
                .append(Component.text("place after a drill\nto auto-collect mined\nblocks.", darkGray))
                .build(),

            // Page 13: Auto Smelter
            Component.text()
                .append(Component.text("Auto Smelter\n", Style.style(TextDecoration.BOLD).color(darkGreen)))
                .append(Component.text("\n"))
                .append(Component.text(AUTO_SMELTER))
                .append(Component.text(" "))
                .append(Component.text("Auto Smelter\n", Style.style(TextDecoration.BOLD).color(darkGreen)))
                .append(Component.text("\nSmelts items passing\nthrough it. Conveyor\nbelt on the bottom,\nfire chamber on top.\n\n", darkGray))
                .append(Component.text("Power cost: ", bold))
                .append(Component.text("2 per item", darkGray))
                .build(),

            // Page 14: Tips
            Component.text()
                .append(Component.text("Tips & Tricks\n", Style.style(TextDecoration.BOLD).color(darkRed)))
                .append(Component.text("\n"))
                .append(Component.text("Lava power pipeline:\n", bold))
                .append(Component.text("Pump > Pipe >\nContainer > Lava Gen\n\n", darkGray))
                .append(Component.text("Auto-mining:\n", bold))
                .append(Component.text("Drill + Conveyor Belt\n+ Auto Smelter for\nauto ore processing\n\n", darkGray))
                .append(Component.text("Placement:\n", bold))
                .append(Component.text("blocks face where you\nlook. The pull direction\nis always from behind.", darkGray))
                .build()
        )
    }
}

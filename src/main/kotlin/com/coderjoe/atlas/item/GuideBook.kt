package com.coderjoe.atlas.item

import com.coderjoe.atlas.block.deposit.Purity
import com.coderjoe.atlas.block.power.mine.MineTier
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.World.Environment
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
    private const val FLUID_PUMP = "\uE105"
    private const val FLUID_PIPE = "\uE106"
    private const val FLUID_CONTAINER = "\uE107"
    private const val CONVEYOR_BELT = "\uE108"

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
                    .color(NamedTextColor.YELLOW),
            )
        }
    }

    fun createRecipe(plugin: JavaPlugin): ShapelessRecipe {
        val key = NamespacedKey(plugin, "atlas_guide")
        val recipe = ShapelessRecipe(key, create())
        recipe.addIngredient(Material.BOOK)
        return recipe
    }

    private fun purityRates(): String =
        Purity.entries.chunked(2).joinToString("\n") { row ->
            row.joinToString("  ") { "${it.displayName} ${it.rateLabel}" }
        }

    private fun mineRates(dimension: Environment): String =
        MineTier.entries
            .filter { it.ore.dimension == dimension }
            .joinToString("\n") { "${it.displayName.removeSuffix(" Mine")} ${it.powerPerHaul} / ${it.cycleSeconds}s" }

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
                .append(
                    Component.text(
                        "\nGenerators produce\npower. Cables transfer\nit. Batteries store it.\n" +
                            "Machines consume it.\n\nWear Atlas Goggles\n" +
                            "and look at a block\nto read it.\n\n",
                        darkGray,
                    ),
                )
                .append(Component.text("Cables: ", bold))
                .append(
                    Component.text(
                        "a whole run acts\nas one network and\njoins itself to what it\ntouches. Nothing needs\naiming.",
                        darkGray,
                    ),
                )
                .build(),
            // Page 3: Small Solar Panel
            Component.text()
                .append(Component.text(SOLAR_PANEL))
                .append(Component.text(" "))
                .append(Component.text("Small Solar Panel\n", Style.style(TextDecoration.BOLD).color(gold)))
                .append(Component.text("\nGenerates 2 power per\n10s during daytime. It\nlights up while it is\nworking.\n\n", darkGray))
                .append(Component.text("Storage: ", bold))
                .append(Component.text("4\n", darkGray))
                .append(Component.text("Tip: ", bold))
                .append(Component.text("power leaves through\nthe base only - put the\ncable underneath.", darkGray))
                .build(),
            // Page 4: Lava Generator
            Component.text()
                .append(Component.text(LAVA_GENERATOR))
                .append(Component.text(" "))
                .append(Component.text("Lava Generator\n", Style.style(TextDecoration.BOLD).color(gold)))
                .append(Component.text("\nGenerates 5 power per\nlava unit consumed. It\nglows while burning.\n\n", darkGray))
                .append(Component.text("Storage: ", bold))
                .append(Component.text("20\n", darkGray))
                .append(Component.text("Input: ", bold))
                .append(Component.text("pulls lava from\nadjacent fluid blocks\non any side.", darkGray))
                .build(),
            // Page 5: Power Cable
            Component.text()
                .append(Component.text(POWER_CABLE))
                .append(Component.text(" "))
                .append(Component.text("Power Cable\n", Style.style(TextDecoration.BOLD).color(gold)))
                .append(Component.text("\nJoins itself to any\npower block it touches.\nNo direction to set.\n\n", darkGray))
                .append(Component.text("Storage: ", bold))
                .append(Component.text("none - a whole run\nmoves power end to\nend each tick\n", darkGray))
                .append(Component.text("Tip: ", bold))
                .append(Component.text("branch freely. A run\nsplits and merges on\nits own.", darkGray))
                .build(),
            // Page 6: Small Battery
            Component.text()
                .append(Component.text(SMALL_BATTERY))
                .append(Component.text(" "))
                .append(Component.text("Small Battery\n", Style.style(TextDecoration.BOLD).color(gold)))
                .append(Component.text("\nStores power for later\nuse. Visual indicator\nshows charge level.\n\n", darkGray))
                .append(Component.text("Storage: ", bold))
                .append(Component.text("50\n", darkGray))
                .append(Component.text("Tip: ", bold))
                .append(Component.text("fills and drains from\nany side, so it needs\nno lining up.", darkGray))
                .build(),
            // Page 7: Fluid System overview
            Component.text()
                .append(Component.text("Fluid System\n", Style.style(TextDecoration.BOLD).color(darkAqua)))
                .append(Component.text("\nPumps extract fluid.\nPipes transport it.\nContainers store it.\n\n", darkGray))
                .append(Component.text("Supported fluids:\n", bold))
                .append(Component.text(" - Water\n - Lava\n\n", darkGray))
                .append(Component.text("Pull-based: ", bold))
                .append(Component.text("same as\npower — each block\npulls from behind.", darkGray))
                .build(),
            // Page 8: Fluid Pump
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
            // Page 9: Fluid Pipe
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
            // Page 10: Fluid Container
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
            // Page 11: Transport System + Conveyor Belt
            Component.text()
                .append(Component.text("Transport System\n", Style.style(TextDecoration.BOLD).color(darkGreen)))
                .append(Component.text("\n"))
                .append(Component.text(CONVEYOR_BELT))
                .append(Component.text(" "))
                .append(Component.text("Conveyor Belt\n", Style.style(TextDecoration.BOLD).color(darkGreen)))
                .append(Component.text("\nMoves dropped items\nin its facing direction.\nNo power required.\n\n", darkGray))
                .append(Component.text("Tip: ", bold))
                .append(Component.text("feed a hopper into a\nvanilla furnace to cook\nores off the belt.", darkGray))
                .build(),
            // Page 12: Mining System
            Component.text()
                .append(Component.text("Mining System\n", Style.style(TextDecoration.BOLD).color(gold)))
                .append(
                    Component.text(
                        "\nA mine is a derrick that\nturns power into the\nore of the chunk it\n" +
                            "stands on. It never\nruns dry.\n\n",
                        darkGray,
                    ),
                )
                .append(Component.text("Output: ", bold))
                .append(Component.text("hands each haul to an\nattached conveyor belt,\nor drops it loose above.", darkGray))
                .build(),
            // Page 13: Ore deposits
            Component.text()
                .append(Component.text("Ore Deposits\n", Style.style(TextDecoration.BOLD).color(gold)))
                .append(
                    Component.text(
                        "\nEvery chunk holds each\nore at a purity, which\nsets how fast a mine\ndrills it:\n\n",
                        darkGray,
                    ),
                )
                .append(Component.text(purityRates() + "\n\n", darkGray))
                .append(Component.text("Goggles: ", bold))
                .append(Component.text("show the\ndeposits where you\nstand.", darkGray))
                .build(),
            // Page 14: Overworld mines
            Component.text()
                .append(Component.text("Overworld Mines\n", Style.style(TextDecoration.BOLD).color(gold)))
                .append(Component.text("\nPower per haul and bore\ntime, Normal deposit:\n\n", darkGray))
                .append(Component.text(mineRates(Environment.NORMAL), darkGray))
                .build(),
            // Page 15: Nether mines
            Component.text()
                .append(Component.text("Nether Mines\n", Style.style(TextDecoration.BOLD).color(gold)))
                .append(Component.text("\nOnly the Nether holds\nthese deposits:\n\n", darkGray))
                .append(Component.text(mineRates(Environment.NETHER), darkGray))
                .build(),
            // Page 16: Tips
            Component.text()
                .append(Component.text("Tips & Tricks\n", Style.style(TextDecoration.BOLD).color(darkRed)))
                .append(Component.text("\n"))
                .append(Component.text("Lava power pipeline:\n", bold))
                .append(Component.text("Pump > Pipe >\nContainer > Lava Gen\n\n", darkGray))
                .append(Component.text("Ore processing:\n", bold))
                .append(Component.text("Mine > Conveyor Belt >\nHopper > Furnace for\ningots\n\n", darkGray))
                .append(Component.text("Placement:\n", bold))
                .append(Component.text("blocks face where you\nlook. The pull direction\nis always from behind.", darkGray))
                .build(),
        )
    }
}

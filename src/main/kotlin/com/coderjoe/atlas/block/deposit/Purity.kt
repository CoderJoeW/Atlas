package com.coderjoe.atlas.block.deposit

/**
 * How rich a chunk's deposit of one [Ore] is, judged against that ore's own spread across the world
 * rather than as a raw count: a Rich diamond chunk is rich for diamonds, not rich next to coal.
 *
 * [rate] is how fast a mine works the deposit, as a multiple of its tier's normal pace. A barren
 * chunk has nothing to dig, so a mine standing on one never starts a bore.
 */
enum class Purity(val displayName: String, val rate: Double) {
    BARREN("Barren", 0.0),
    POOR("Poor", 0.5),
    NORMAL("Normal", 1.0),
    RICH("Rich", 1.5),
    PURE("Pure", 2.0),
    ;

    /** [rate] as a player reads it, e.g. `1.5x` or `2x`. */
    val rateLabel: String get() = "${rate.toString().removeSuffix(".0")}x"
}

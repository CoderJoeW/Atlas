package com.coderjoe.atlas.pack

import com.coderjoe.atlas.testing.AtlasPaths.configFiles
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Guards the client-side block state budget.
 *
 * Every appearance in a CraftEngine config claims one vanilla block state out of a fixed pool, and
 * the server refuses to start the pack when a pool overflows. That failure only shows up at
 * runtime, so it is checked here instead: the auto-connecting cable alone claims 128 states.
 */
class AutoStateBudgetTest {
    private companion object {
        /**
         * Usable slots per group, as reported by `/ce debug auto-state-usage <group>`.
         *
         * These are ACTIVE candidates, not the raw candidate count. Two traps live here:
         *
         * Groups share one candidate array indexed by registry id, so overlapping groups
         * ('leaves' and 'non_tintable_leaves') do NOT add up. 'waterlogged_leaves' is disjoint
         * and looks tempting, but the client renders water inside a waterlogged state, so a
         * block using it appears submerged - never use it for a dry block.
         *
         * CraftEngine also caches appearance-to-state assignments in
         * plugins/CraftEngine/cache/visual_block_states.json and keeps reserving slots for
         * appearance names that no longer exist. Retiring a block leaks its states until that
         * cache is pruned, which makes a group look full when it is not.
         *
         * These pools are shared with every other pack installed on the server, and this test
         * only sees Atlas's own configs. On the dev server two leaves and seven tripwire slots
         * belong to another pack, so passing here is necessary but not sufficient - a config can
         * still be refused at load. Treat a near-full group as full.
         *
         * 'chorus' is absent on purpose. CraftEngine ships its 63 mappings commented out with the
         * note that the hitbox is "super weird", so it is not a pool we draw from for anything a
         * player walks past or clicks on.
         *
         * These numbers are APPEARANCES, not raw vanilla states, and cave_vines is where that
         * bites: the server refuses with "maximum capacity of '96' slots", but an appearance
         * there costs two states (cave_vines + cave_vines_plant), so it fits 48. Reading the
         * slot count out of the error message as if it were appearances overshoots by double.
         */
        val CAPACITY =
            mapOf(
                "leaves" to 117,
                "tripwire" to 126,
                "solid" to 1489,
                "cave_vines" to 48,
                "cactus" to 16,
                "sugar_cane" to 15,
                "weeping_vines" to 25,
                "twisting_vines" to 25,
            )

        /**
         * Slots held by packs OTHER than Atlas, measured from the live server's
         * `run/plugins/CraftEngine/cache/visual_block_states.json`: group the reservations by
         * vanilla host and count only the ones whose owning block id is not `atlas:`.
         *
         * Without subtracting these the test compares Atlas's configs against the whole pool and
         * goes green while the server refuses the pack. Counting every reservation on a host as
         * foreign is just as wrong in the other direction - most of the tripwire pool is Atlas's
         * own power cable, not another pack. Re-measure after installing or removing a pack; see
         * docs/design/fluid-pipe/README.md.
         */
        val FOREIGN =
            mapOf(
                "tripwire" to 7,
                "leaves" to 2,
                "sugar_cane" to 7,
                "cactus" to 2,
            )

        val AUTO_STATE = Regex("""auto-state:\s*(\S+)""")
    }

    @Test
    fun `no auto-state group is over its capacity`() {
        val configs = configFiles()
        assertTrue(configs.isNotEmpty(), "no configuration files found")

        val used = mutableMapOf<String, Int>()
        for (config in configs) {
            for (line in config.readLines()) {
                val group = AUTO_STATE.find(line)?.groupValues?.get(1) ?: continue
                used[group] = (used[group] ?: 0) + 1
            }
        }

        for ((group, count) in used) {
            val capacity =
                CAPACITY[group]
                    ?: error("unknown auto-state group '$group' - add its capacity to this test")
            val share = capacity - (FOREIGN[group] ?: 0)
            assertTrue(
                count <= share,
                "auto-state group '$group' is over budget: $count appearances used of $share " +
                    "available to Atlas ($capacity in the pool, ${FOREIGN[group] ?: 0} held by other packs)",
            )
        }
    }
}

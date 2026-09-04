package com.coderjoe.atlas

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

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
         */
        val CAPACITY =
            mapOf(
                "leaves" to 117,
                "tripwire" to 126,
                "solid" to 1489,
                // Measured from what CraftEngine actually allocated, not from raw state counts:
                // cave_vines is age 0-25 x berries, so ~50 usable slots rather than 100.
                "cave_vines" to 50,
                // untinted, never waterlogged, and nothing else was using it
                "cactus" to 16,
                "weeping_vines" to 25,
                "twisting_vines" to 25,
            )

        val AUTO_STATE = Regex("""auto-state:\s*(\S+)""")
    }

    @Test
    fun `no auto-state group is over its capacity`() {
        val configs = File("src/main/resources/atlas/configuration").listFiles { f -> f.extension == "yml" }
        assertTrue(configs != null && configs.isNotEmpty(), "no configuration files found")

        val used = mutableMapOf<String, Int>()
        for (config in configs!!) {
            for (line in config.readLines()) {
                val group = AUTO_STATE.find(line)?.groupValues?.get(1) ?: continue
                used[group] = (used[group] ?: 0) + 1
            }
        }

        for ((group, count) in used) {
            val capacity =
                CAPACITY[group]
                    ?: error("unknown auto-state group '$group' - add its capacity to this test")
            assertTrue(
                count <= capacity,
                "auto-state group '$group' is over budget: $count states used of $capacity available",
            )
        }
    }
}

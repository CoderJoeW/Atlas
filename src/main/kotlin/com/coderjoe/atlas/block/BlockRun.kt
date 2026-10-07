package com.coderjoe.atlas.block

import org.bukkit.block.BlockFace

/**
 * One connected run of [T] found by a single flood fill, together with every other block touching
 * it. [links] holds, for each member, the indices of the members against it, in face order.
 */
class BlockRun<T : AtlasBlock>(
    val members: List<T>,
    val edges: List<Edge>,
    val links: List<IntArray>,
) {
    /** A block touching the run without being part of it, and the face pointing from it back at the member it touches. */
    data class Edge(val memberIndex: Int, val block: AtlasBlock, val faceTowardRun: BlockFace)

    companion object {
        fun <T : AtlasBlock> discover(
            start: T,
            type: Class<T>,
        ): BlockRun<T> {
            val indexOf = HashMap<AtlasBlock, Int>()
            val members = ArrayList<T>()
            val links = ArrayList<IntArray>()
            val edges = ArrayList<Edge>()
            indexOf[start] = 0
            members += start

            var next = 0
            while (next < members.size) {
                val member = members[next]
                val linked = ArrayList<Int>(AtlasBlock.ADJACENT_FACES.size)
                for (face in AtlasBlock.ADJACENT_FACES) {
                    val neighbor = member.neighbor(face) ?: continue
                    if (!type.isInstance(neighbor)) {
                        edges += Edge(next, neighbor, face.oppositeFace)
                        continue
                    }
                    linked +=
                        indexOf.getOrPut(neighbor) {
                            members += type.cast(neighbor)
                            members.size - 1
                        }
                }
                links += linked.toIntArray()
                next++
            }
            return BlockRun(members, edges, links)
        }
    }
}

/**
 * Keeps one [R] per connected run of [T], shared by every member of the run.
 *
 * A run is only rediscovered after its shape may have changed: a member placed or removed, or a
 * block placed or removed against one. The registry reports both, and either one drops the whole
 * run so that the next [runFor] or [runs] rebuilds it with one flood fill. Splitting and merging
 * need no special handling - the pieces are simply found again.
 */
class RunCache<T : AtlasBlock, R : Any>(
    private val type: Class<T>,
    private val build: (BlockRun<T>) -> R,
) {
    private val runOf = HashMap<T, R>()
    private val membersOf = LinkedHashMap<R, List<T>>()
    private val unassigned = LinkedHashSet<T>()

    fun runFor(member: T): R = runOf[member] ?: rebuild(member)

    /** The run [member] already belongs to, without building one. */
    fun cached(member: T): R? = runOf[member]

    /** Every run, building any that are waiting on a member placed or changed since. */
    fun runs(): List<R> {
        while (unassigned.isNotEmpty()) rebuild(unassigned.first())
        return membersOf.keys.toList()
    }

    fun placed(member: T) {
        invalidate(member)
        unassigned += member
    }

    fun removed(member: T) {
        invalidate(member)
        unassigned -= member
    }

    fun invalidate(member: T) {
        val run = runOf[member] ?: return
        val members = membersOf.remove(run) ?: return
        for (stale in members) {
            runOf -= stale
            unassigned += stale
        }
    }

    private fun rebuild(start: T): R {
        val found = BlockRun.discover(start, type)
        found.members.forEach(::invalidate)
        val run = build(found)
        for (member in found.members) {
            runOf[member] = run
            unassigned -= member
        }
        membersOf[run] = found.members
        return run
    }
}

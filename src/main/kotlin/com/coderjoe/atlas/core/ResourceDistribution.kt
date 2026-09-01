package com.coderjoe.atlas.core

import org.bukkit.block.BlockFace

fun <T : AtlasBlock> pullFromOpposite(
    facing: BlockFace,
    getAdjacent: (BlockFace) -> T?,
    canPullSelf: () -> Boolean,
    canProvide: (source: T) -> Boolean,
    transfer: (source: T) -> Unit,
) {
    if (!canPullSelf()) return
    val source = getAdjacent(facing.oppositeFace) ?: return
    if (!canProvide(source)) return
    transfer(source)
}

fun <T : AtlasBlock> pushRoundRobin(
    excludeFace: BlockFace,
    startIndex: Int,
    getAdjacent: (BlockFace) -> T?,
    hasResource: () -> Boolean,
    isCandidate: (target: T) -> Boolean,
    tryPush: (target: T, face: BlockFace) -> Boolean,
    stopAfterFirstCandidate: Boolean = false,
): Int =
    pushRoundRobinTo(
        outputFaces = AtlasBlock.ADJACENT_FACES.filter { it != excludeFace },
        startIndex = startIndex,
        getAdjacent = getAdjacent,
        hasResource = hasResource,
        isCandidate = isCandidate,
        tryPush = tryPush,
        stopAfterFirstCandidate = stopAfterFirstCandidate,
    )

/** Round-robins over an explicit set of output faces, for blocks that feed only some sides. */
fun <T : AtlasBlock> pushRoundRobinTo(
    outputFaces: List<BlockFace>,
    startIndex: Int,
    getAdjacent: (BlockFace) -> T?,
    hasResource: () -> Boolean,
    isCandidate: (target: T) -> Boolean,
    tryPush: (target: T, face: BlockFace) -> Boolean,
    stopAfterFirstCandidate: Boolean = false,
): Int {
    val faceCount = outputFaces.size
    var lastPushOffset = -1

    for (i in outputFaces.indices) {
        if (!hasResource()) break
        val face = outputFaces[(startIndex + i) % faceCount]
        val target = getAdjacent(face) ?: continue
        if (!isCandidate(target)) continue
        if (tryPush(target, face)) {
            lastPushOffset = i
        }
        if (stopAfterFirstCandidate) break
    }

    return if (lastPushOffset >= 0) (startIndex + lastPushOffset + 1) % faceCount else startIndex
}

fun <T : AtlasBlock> pullFromAll(
    excludeFace: BlockFace,
    getAdjacent: (BlockFace) -> T?,
    isDone: () -> Boolean,
    isCandidate: (source: T, face: BlockFace) -> Boolean,
    tryPull: (source: T, face: BlockFace) -> Boolean,
    stopAfterFirstCandidate: Boolean = false,
) = pullFromFaces(
    inputFaces = AtlasBlock.ADJACENT_FACES.filter { it != excludeFace },
    getAdjacent = getAdjacent,
    isDone = isDone,
    isCandidate = isCandidate,
    tryPull = tryPull,
    stopAfterFirstCandidate = stopAfterFirstCandidate,
)

/** Pulls from an explicit set of input faces, for blocks that collect from only some sides. */
fun <T : AtlasBlock> pullFromFaces(
    inputFaces: List<BlockFace>,
    getAdjacent: (BlockFace) -> T?,
    isDone: () -> Boolean,
    isCandidate: (source: T, face: BlockFace) -> Boolean,
    tryPull: (source: T, face: BlockFace) -> Boolean,
    stopAfterFirstCandidate: Boolean = false,
) {
    for (face in inputFaces) {
        if (isDone()) break
        val source = getAdjacent(face) ?: continue
        if (!isCandidate(source, face)) continue
        tryPull(source, face)
        if (stopAfterFirstCandidate) break
    }
}

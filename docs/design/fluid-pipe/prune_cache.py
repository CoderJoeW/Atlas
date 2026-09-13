"""Drops a block's cached appearance->vanilla-state reservations, with the server STOPPED.

CraftEngine caches every appearance-to-state assignment in
`plugins/CraftEngine/cache/visual_block_states.json` and never releases one on its own. Two
things go wrong because of that:

1. Changing an appearance's `auto-state` group does nothing until its cached entry is dropped -
   the old assignment wins, so a rebalance silently has no effect.
2. Reservations accumulate across loads and squat on a pool, so a group reports itself full when
   the live configs would fit comfortably. That is what made the fluid pipe look like it had run
   out of room when it had not.

Run with the server stopped, then start it - CraftEngine reallocates whatever it dropped.

    python3 prune_cache.py run/plugins/CraftEngine/cache/visual_block_states.json atlas:fluid_pipe

Pass no block id to just report what is reserved and by whom.
"""
import collections
import json
import sys


def report(cache: dict) -> None:
    by_host = collections.Counter()
    foreign = collections.Counter()
    for key, value in cache.items():
        host = value.split("[")[0].replace("minecraft:", "")
        by_host[host] += 1
        if not key.startswith("atlas:"):
            foreign[host] += 1

    print(f"{len(cache)} reservations\n")
    print(f"{'host':28}{'total':>7}{'other packs':>13}")
    for host, total in by_host.most_common():
        print(f"{host:28}{total:>7}{foreign[host]:>13}")
    print(
        "\nAn appearance is not always one slot: cave_vines costs two states"
        " (cave_vines + cave_vines_plant), so its '96 slots' is 48 appearances."
    )


def main() -> None:
    path = sys.argv[1]
    cache = json.load(open(path))

    if len(sys.argv) < 3:
        report(cache)
        return

    block = sys.argv[2]
    stale = [k for k in cache if k.startswith(f"{block}[")]
    for key in stale:
        del cache[key]
    json.dump(cache, open(path, "w"))
    print(f"dropped {len(stale)} reservations for {block} ({len(cache)} left)")


if __name__ == "__main__":
    main()

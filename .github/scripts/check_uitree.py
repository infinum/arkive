#!/usr/bin/env python3
"""Fails when a showcase snapshot lacks a parseable UI tree. Run after generateWebShowcase."""
import json
import os
import sys

root = sys.argv[1] if len(sys.argv) > 1 else "samples/build/generated/arkive/showcase"
with open(os.path.join(root, "arkive-showcase.json")) as handle:
    showcase = json.load(handle)

shots, problems = 0, []
for module in showcase["modules"]:
    for item in module["items"]:
        for shot in [item] + item["variants"]:
            shots += 1
            path = shot.get("uiTreePath")
            if not path:
                problems.append(f"{module['name']}: no ui tree for {shot['snapshotPath']}")
                continue
            tree_file = os.path.join(root, module["name"], "uitree", os.path.basename(path))
            try:
                with open(tree_file) as handle:
                    tree = json.load(handle)
                if tree.get("schemaVersion") != 1 or len(tree["root"]["bounds"]) != 4:
                    problems.append(f"{module['name']}: unexpected schema in {tree_file}")
            except (OSError, ValueError, KeyError) as error:
                problems.append(f"{module['name']}: unreadable {tree_file}: {error}")

print(f"ui trees: {shots - len(problems)}/{shots} snapshots paired")
for problem in problems:
    print(problem)
sys.exit(1 if problems else 0)

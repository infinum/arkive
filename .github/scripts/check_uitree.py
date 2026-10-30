#!/usr/bin/env python3
"""Fails when a showcase snapshot lacks a parseable UI tree, or when the known sample trees
lose content the inspector and skills rely on. Run after generateWebShowcase."""
import json
import os
import re
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
if shots == 0:
    problems.append("no snapshots in the showcase")


def walk(node, parent=None):
    yield node, parent
    for child in node.get("children", []):
        yield from walk(child, node)


def load_sample_tree(name):
    with open(os.path.join(root, "sample", "uitree", f"com-infinum-arkive-sample-components-{name}.json")) as handle:
        tree = json.load(handle)
    return tree, list(walk(tree["root"]))


def check_source(tree, nodes):
    return any(re.match(r"^Buttons\.kt:\d+$", node.get("source", "")) for node, _ in nodes)


def check_text_style(tree, nodes):
    styles = [node.get("textStyle", {}) for node, _ in nodes if node.get("text") == "Start hike"]
    return any(style.get("overflow") is False and style.get("fontSize") == 16 for style in styles)


def check_outer_box(tree, nodes):
    rows = [node for node, _ in nodes if node.get("composable") == "Row" and "PrimaryButton" in node.get("callers", [])]
    return any(abs(row["bounds"][2] - tree["size"][0]) <= 1 for row in rows)


def check_zero_area_origin(tree, nodes):
    return any(
        (b[2] == 0 or b[3] == 0) and (b[0] != 0 or b[1] != 0)
        for b in (node["bounds"] for node, _ in nodes)
    )


def check_hosted_view(tree, nodes):
    return any(
        node.get("view") == "TextView" and node.get("text") == "From a TextView"
        and parent is not None and parent.get("composable") == "AndroidView"
        for node, parent in nodes
    )


CONTENT_CHECKS = [
    ("previewprimarybutton", "a node's source is Buttons.kt:<line>", check_source),
    ("previewprimarybutton", '"Start hike" text has fontSize 16 and overflow false', check_text_style),
    ("previewprimarybutton", "PrimaryButton's Row spans the snapshot width (outer box)", check_outer_box),
    ("previewprimarybutton", "a zero-area node keeps its non-zero origin", check_zero_area_origin),
    ("previewlegacybadge", 'TextView "From a TextView" sits under an AndroidView node', check_hosted_view),
]

# Content checks only apply to this repo's Peak sample; a consumer-shaped showcase skips them.
sample = next((module for module in showcase["modules"] if module["name"] == "sample"), None)
if sample and any(item["component"]["packageName"].startswith("com.infinum.arkive.sample") for item in sample["items"]):
    for tree_name, description, check in CONTENT_CHECKS:
        try:
            passed = check(*load_sample_tree(tree_name))
        except (OSError, ValueError, KeyError, IndexError, TypeError) as error:
            passed, description = False, f"{description} ({error})"
        print(f"content check {'ok  ' if passed else 'FAIL'}: {tree_name}: {description}")
        if not passed:
            problems.append(f"sample: content check failed: {tree_name}: {description}")
else:
    print("content checks: skipped (the arkive sample module is not in this showcase)")

for problem in problems:
    print(problem)
sys.exit(1 if problems else 0)

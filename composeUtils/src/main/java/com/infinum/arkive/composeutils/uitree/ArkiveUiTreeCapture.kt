package com.infinum.arkive.composeutils.uitree

import androidx.compose.runtime.Composable

/**
 * Wraps one snapshot's content. When the Arkive Gradle plugin enabled UI-tree capture (the
 * `arkive.uitree.dir` system property on the test JVM) it writes `<dir>/<shotId>.json` — the
 * hierarchy, dp bounds, semantics and text style of the frame the engine captures, whichever
 * engine that is. Without the property it is a pure pass-through and no class touching
 * ui-tooling-data loads, so a consumer's own tests are unaffected. Called by Arkive's
 * generated shooter; not meant for direct use.
 *
 * @param shotId identifies the snapshot; the tree is written as `<shotId>.json`.
 * @param appPackages the module's own packages: calls made from them get a `source` location.
 * @param content the snapshot's content, laid out exactly as it would be unwrapped.
 */
@Composable
fun ArkiveUiTreeCapture(
    shotId: String,
    appPackages: Set<String>,
    content: @Composable () -> Unit,
) {
    val directory = UiTreeFiles.directory()
    if (directory == null) {
        content()
    } else {
        UiTreeRecorder(shotId, appPackages, directory, content)
    }
}

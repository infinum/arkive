@file:OptIn(InternalComposeApi::class)

package com.infinum.arkive.composeutils.uitree

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.tooling.CompositionData
import androidx.compose.runtime.tooling.LocalInspectionTables
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import java.io.File

/** Marks the capture root in the semantics tree so the walker can find its layout node. */
internal val CaptureRootKey = SemanticsPropertyKey<Unit>("ArkiveUiTreeRoot")

/**
 * The recording half of [ArkiveUiTreeCapture], in its own file so the pass-through path never
 * loads it (or, through it, ui-tooling-data). Mirrors how ui-tooling's preview host records
 * composition data — parameter/source markers on, LocalInspectionTables provided so
 * sub-compositions (lazy items, Scaffold slots) register too — but never sets
 * LocalInspectionMode, which would change what previews render.
 */
@Composable
internal fun UiTreeRecorder(
    shotId: String,
    appPackages: Set<String>,
    directory: File,
    content: @Composable () -> Unit,
) {
    // These runtime APIs are internal and may change under a future Compose: compute the
    // setup once, outside composition of the content (try/catch cannot wrap composable
    // calls), and on failure render the preview untouched rather than fail the shot.
    val composer = currentComposer
    val tables = remember { mutableSetOf<CompositionData>() }
    val inspectionTables = remember {
        runCatching {
            composer.collectParameterInformation()
            tables.add(composer.compositionData)
            LocalInspectionTables provides tables
        }.onFailure { e -> println("Arkive: ui tree skipped for $shotId: ${e.message ?: e}") }.getOrNull()
    }
    if (inspectionTables == null) {
        content()
        return
    }
    val view = LocalView.current
    val session = remember(shotId, directory) {
        val walker = ComposeUiTreeWalker(shotId, view, tables, AppPackages(appPackages))
        UiTreeSession(shotId, directory, walker::walk)
    }
    DisposableEffect(session) {
        session.start()
        // Any applied state change may move layout, so the next draw re-walks. Draws without
        // changes (e.g. the engine's capture draw of a settled frame) reuse the last tree.
        val observer = Snapshot.registerApplyObserver { _, _ -> session.markDirty() }
        onDispose {
            session.finish()
            observer.dispose()
        }
    }
    CompositionLocalProvider(inspectionTables) {
        Layout(
            content = content,
            modifier = Modifier
                .semantics { this[CaptureRootKey] = Unit }
                .drawWithContent {
                    drawContent()
                    session.onDrawn()
                },
            measurePolicy = PassThroughMeasurePolicy,
        )
    }
}

/**
 * Lays children out the way the compose root does — measured with the incoming constraints,
 * stacked at the origin, sized to the largest child — so wrapping a preview never moves a
 * pixel: goldens recorded without UI-tree capture still verify with it.
 */
private object PassThroughMeasurePolicy : MeasurePolicy {
    override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
        val placeables = measurables.map { it.measure(constraints) }
        val width = constraints.constrainWidth(placeables.maxOfOrNull { it.width } ?: 0)
        val height = constraints.constrainHeight(placeables.maxOfOrNull { it.height } ?: 0)
        return layout(width, height) {
            placeables.forEach { it.placeRelative(0, 0) }
        }
    }
}

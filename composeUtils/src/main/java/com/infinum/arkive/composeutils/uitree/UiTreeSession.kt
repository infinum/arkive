package com.infinum.arkive.composeutils.uitree

import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Turns draws into `<directory>/<shotId>.json`. The engine's capture is a draw, so the last
 * draw is the captured frame: every draw that follows a change re-walks, and draws without
 * changes keep the last tree. A failing walk is logged once and deletes the file — a tree
 * from an earlier frame or run must never be paired with this snapshot.
 */
internal class UiTreeSession(
    private val shotId: String,
    private val directory: File,
    private val walk: () -> UiTree,
    private val log: (String) -> Unit = ::println,
) {
    private val dirty = AtomicBoolean(true)
    private val file = File(directory, "$shotId.json")
    private var lastWritten: String? = null
    private var reportedFailure = false
    private var walkAttempted = false
    private var reportedNothingDrawn = false

    fun start() {
        file.delete()
    }

    /**
     * Ends the shot. A capture root that never drew (dialog-only previews draw in another
     * window) never walked, so say why there is no tree.
     */
    fun finish() {
        if (!walkAttempted && !reportedNothingDrawn) {
            reportedNothingDrawn = true
            log("Arkive: ui tree skipped for $shotId: nothing was drawn in the captured window")
        }
    }

    fun markDirty() {
        dirty.set(true)
    }

    fun onDrawn() {
        if (!dirty.getAndSet(false)) return
        walkAttempted = true
        try {
            val json = UiTreeJson.write(walk())
            if (json != lastWritten) {
                directory.mkdirs()
                file.writeText(json)
                lastWritten = json
            }
        } catch (e: Throwable) {
            file.delete()
            lastWritten = null
            if (!reportedFailure) {
                reportedFailure = true
                log("Arkive: ui tree skipped for $shotId: ${e.message ?: e}")
            }
        }
    }
}

package com.infinum.arkive.samplecmp

// KSP skips a compilation that has zero sources of its own (NO-SOURCE), which would
// prevent Arkive's test processor from generating the snapshot test. This file exists
// solely to give the androidHostTest compilation a source file; any real test would
// serve the same purpose.
internal object ArkivePlaceholder

package com.infinum.arkive.plugin.extensions

/**
 * Which recorded snapshots stay in Paparazzi's `src/test/snapshots` golden directory
 * after the showcase consumes them. The showcase itself always receives everything.
 */
enum class SnapshotRetention {
    /** Consume all snapshots; the golden directory is left empty (default). */
    NONE,

    /**
     * Keep only base snapshots as goldens — cheap `verifyPaparazzi` coverage without
     * committing every font/density/layout-direction variant to the repository.
     * Verify against them with:
     * `./gradlew verifyPaparazzi<Variant> --tests '*.testAllComposableFunctions'`
     */
    BASE,

    /** Keep every snapshot, including variants. Consider Git LFS for the golden dir. */
    ALL,
}

package com.infinum.arkive.plugin.generators

import com.infinum.arkive.metadata.model.Component
import com.infinum.arkive.metadata.model.ComponentsMetaData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShowcaseGeneratorImplTest {

    private val missing = mutableListOf<String>()
    private val malformed = mutableListOf<String>()
    private val generator = ShowcaseGeneratorImpl(
        onMissingSnapshot = { missing += it },
        onMalformedVariant = { malformed += it },
    )

    @Test
    fun baseAndVariantsAreMatchedToTheirComponent() {
        val id = "com-app-ui-card"
        val items = generator.generateShowcase(
            snapshots = listOf(
                snapshot(id),
                snapshot("${id}_font_1.5"),
                snapshot("${id}_layoutDirection_LTR"),
            ),
            metadata = metadata(id),
        )

        assertEquals(1, items.size)
        assertEquals(snapshot(id), items.single().snapshotPath)
        assertEquals(
            listOf("font" to "1.5", "layoutDirection" to "LTR"),
            items.single().variants.map { it.category to it.variant },
        )
    }

    // The review's failure scenario: a Card preview in com.app.ui and a Header preview in
    // com.app.ui.card. With '_'-joined ids the second component's base snapshot was claimed
    // as a variant of the first and crashed variant parsing with IndexOutOfBoundsException.
    @Test
    fun componentWhoseIdExtendsAnotherIdIsNotClaimedAsItsVariant() {
        val card = "com-app-ui-card"
        val header = "com-app-ui-card-header"
        val items = generator.generateShowcase(
            snapshots = listOf(snapshot(card), snapshot(header)),
            metadata = metadata(card, header),
        )

        assertEquals(listOf(card, header), items.map { it.component.id })
        assertTrue(items.all { it.variants.isEmpty() })
        assertTrue(malformed.isEmpty())
    }

    @Test
    fun idMatchingIsAnchoredAtTheFilenameSeparator() {
        val items = generator.generateShowcase(
            snapshots = listOf(snapshot("xcom-foo-card")),
            metadata = metadata("com-foo-card"),
        )

        assertTrue(items.isEmpty())
        assertEquals(listOf("com-foo-card"), missing)
    }

    @Test
    fun malformedVariantNameIsSkippedInsteadOfCrashing() {
        val id = "com-app-ui-card"
        val items = generator.generateShowcase(
            snapshots = listOf(snapshot(id), snapshot("${id}_orphan")),
            metadata = metadata(id),
        )

        assertEquals(1, items.size)
        assertTrue(items.single().variants.isEmpty())
        assertEquals(listOf(snapshot("${id}_orphan")), malformed)
    }

    @Test
    fun variantValueMayContainUnderscores() {
        val id = "com-app-ui-card"
        val items = generator.generateShowcase(
            snapshots = listOf(snapshot(id), snapshot("${id}_user_state_0")),
            metadata = metadata(id),
        )

        assertEquals(
            listOf("user" to "state_0"),
            items.single().variants.map { it.category to it.variant },
        )
    }

    private fun snapshot(name: String) = "images/$TEST_CLASS_PREFIX$name.png"

    private fun metadata(vararg ids: String) = ComponentsMetaData(
        components = ids.map { id ->
            Component(
                id = id,
                name = id,
                functionName = "",
                packageName = "",
                fileName = "",
                group = "",
                tags = emptyList(),
                extraMetadata = emptyList(),
            )
        },
    )

    companion object {
        private const val TEST_CLASS_PREFIX =
            "com.infinum.arkive_ArkiveSnapshotTestGenerator_testAllComposableFunctions_"
    }
}

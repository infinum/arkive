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

    // Function names may contain '_', so Card_Header's id extends Card's at a '_' and every
    // Card_Header file contains Card's "_<id>_" marker. Its variants used to be attached to
    // Card as category "header", value "font_1.5".
    @Test
    fun componentWhoseNameExtendsAnotherAtUnderscoreKeepsItsOwnSnapshots() {
        val card = "com-app-ui-card"
        val header = "com-app-ui-card_header"
        val items = generator.generateShowcase(
            snapshots = listOf(
                snapshot(card),
                snapshot("${card}_font_1.5"),
                snapshot(header),
                snapshot("${header}_font_1.5"),
                snapshot("${header}_density_ldpi"),
            ),
            metadata = metadata(card, header),
        )

        assertEquals(
            listOf(
                card to listOf("font" to "1.5"),
                header to listOf("font" to "1.5", "density" to "ldpi"),
            ),
            items.map { item -> item.component.id to item.variants.map { it.category to it.variant } },
        )
        assertEquals(snapshot(header), items.last().snapshotPath)
        assertTrue(malformed.isEmpty())
    }

    // The extension is itself shaped like a variant ("_header_dark" reads as category
    // "header", value "dark"), so only the longest-id-first rule keeps it with its owner.
    @Test
    fun baseShapedLikeAnotherComponentsVariantGoesToItsOwner() {
        val card = "com-app-ui-card"
        val headerDark = "com-app-ui-card_header_dark"
        val items = generator.generateShowcase(
            snapshots = listOf(snapshot(card), snapshot(headerDark)),
            metadata = metadata(card, headerDark),
        )

        assertEquals(listOf(card, headerDark), items.map { it.component.id })
        assertTrue(items.all { it.variants.isEmpty() })
    }

    // A package segment may contain '_' too (com.app.ui.card_list + Item).
    @Test
    fun packageWithUnderscoreDoesNotLeakVariantsIntoAShorterId() {
        val card = "com-app-ui-card"
        val listItem = "com-app-ui-card_list-item"
        val items = generator.generateShowcase(
            snapshots = listOf(snapshot(card), snapshot(listItem), snapshot("${listItem}_font_2.0")),
            metadata = metadata(card, listItem),
        )

        assertTrue(items.first().variants.isEmpty())
        assertEquals(listOf("font" to "2.0"), items.last().variants.map { it.category to it.variant })
    }

    // Categories and values never contain '_' (preview-parameter categories are emitted as
    // "param-<name>" with '_' → '-'), so a longer remainder is not a variant of this id —
    // e.g. a retained golden of a since-deleted Card_Header must not attach to Card.
    @Test
    fun variantSuffixMustBeExactlyCategoryAndValue() {
        val id = "com-app-ui-card"
        val items = generator.generateShowcase(
            snapshots = listOf(snapshot(id), snapshot("${id}_header_font_1.5")),
            metadata = metadata(id),
        )

        assertTrue(items.single().variants.isEmpty())
        assertEquals(listOf(snapshot("${id}_header_font_1.5")), malformed)
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

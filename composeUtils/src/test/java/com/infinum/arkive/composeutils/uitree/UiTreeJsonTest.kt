package com.infinum.arkive.composeutils.uitree

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UiTreeJsonTest {

    @Test
    fun writesThePinnedSchemaShape() {
        val tree = UiTree(
            id = "com-app-card",
            density = 2.625f,
            width = 360f,
            height = 48f,
            root = UiTreeNode(
                bounds = UiBounds(0f, 0f, 360f, 48f),
                composable = "Row",
                callers = listOf("PrimaryButton"),
                source = "Buttons.kt:39",
                children = listOf(
                    UiTreeNode(
                        bounds = UiBounds(48f, 14f, 120f, 20f),
                        composable = "Text",
                        source = "Buttons.kt:50",
                        text = "Pay",
                        textStyle = UiTextStyle(
                            fontSize = 16f,
                            fontWeight = 500,
                            lineHeight = 20f,
                            color = "#FFFFFFFF",
                            maxLines = 1,
                            overflow = false,
                        ),
                    ),
                ),
            ),
        )

        val expected = """
            {
              "schemaVersion": 1,
              "id": "com-app-card",
              "density": 2.625,
              "size": [360, 48],
              "root": {
                "composable": "Row",
                "callers": ["PrimaryButton"],
                "source": "Buttons.kt:39",
                "bounds": [0, 0, 360, 48],
                "children": [
                  {
                    "composable": "Text",
                    "source": "Buttons.kt:50",
                    "bounds": [48, 14, 120, 20],
                    "text": "Pay",
                    "textStyle": {
                      "fontSize": 16,
                      "fontWeight": 500,
                      "lineHeight": 20,
                      "color": "#FFFFFFFF",
                      "maxLines": 1,
                      "overflow": false
                    }
                  }
                ]
              }
            }
        """.trimIndent() + "\n"
        assertEquals(expected, UiTreeJson.write(tree))
    }

    @Test
    fun omitsNullAndEmptyFields() {
        val json = UiTreeJson.write(UiTree("x", 1f, 10f, 10f, UiTreeNode(bounds = UiBounds(0f, 0f, 10f, 10f))))

        assertFalse(json, json.contains("children"))
        assertFalse(json, json.contains("callers"))
        assertFalse(json, json.contains("null"))
        assertTrue(json, json.contains("\"bounds\": [0, 0, 10, 10]"))
    }

    @Test
    fun escapesStrings() {
        val node = UiTreeNode(bounds = UiBounds(0f, 0f, 1f, 1f), text = "He said \"hi\"\\\n\tok\u0001")
        val json = UiTreeJson.write(UiTree("x", 1f, 1f, 1f, node))

        assertTrue(json, json.contains("\"text\": \"He said \\\"hi\\\"\\\\\\n\\tok\\u0001\""))
    }

    @Test
    fun formatsNumbersCompactly() {
        assertEquals("16", UiTreeJson.formatNumber(16f))
        assertEquals("16.5", UiTreeJson.formatNumber(16.5f))
        assertEquals("16.19", UiTreeJson.formatNumber(16.190476f))
        assertEquals("0", UiTreeJson.formatNumber(-0f))
        assertEquals("360", UiTreeJson.formatNumber(360f))
        assertEquals("2.625", UiTreeJson.formatNumber(2.625f, decimals = 4))
    }
}

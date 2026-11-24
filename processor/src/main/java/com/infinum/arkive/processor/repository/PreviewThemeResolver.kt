package com.infinum.arkive.processor.repository

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import com.infinum.arkive.processor.models.PreviewTheme
import com.squareup.kotlinpoet.MemberName

/**
 * Resolves the `arkive.previewTheme` / `arkive.previewThemes` KSP args (fully-qualified
 * top-level composables) to [PreviewTheme]s. Resolution goes through the classpath, so a
 * theme may live in another module (e.g. a design-system library). Misconfiguration is a
 * KSP error, never a silent fallback to unthemed snapshots.
 */
internal class PreviewThemeResolver(
    private val resolver: Resolver,
    private val logger: KSPLogger,
) {

    /**
     * The module's themes in skin order, the first being the default: the named
     * [previewThemes] (`name=fqn;name=fqn`), else the single [previewTheme], else none.
     */
    internal fun resolveAll(previewTheme: String, previewThemes: String): List<PreviewTheme> {
        val named = previewThemes.split(';').filter { it.isNotBlank() }
        return when {
            named.isNotEmpty() && previewTheme.isNotBlank() -> {
                logger.error("Arkive: set either previewTheme or previewThemes, not both.")
                emptyList()
            }
            named.isNotEmpty() -> named.mapNotNull { resolveSkin(it) }
            previewTheme.isBlank() -> emptyList()
            else -> listOfNotNull(resolve(DEFAULT_NAME, previewTheme))
        }
    }

    private fun resolveSkin(entry: String): PreviewTheme? {
        val name = entry.substringBefore('=')
        if (!name.matches(SKIN_NAME)) {
            logger.error("Arkive: skin name \"$name\" must be letters and digits only (it ends up in filenames).")
            return null
        }
        return resolve(name, entry.substringAfter('='))
    }

    // A theme reference is `com.example.AppTheme` or, to pin parameters per skin,
    // `com.example.AppTheme(brand = com.example.Brand.RED)` — arguments are named and passed
    // verbatim, so values must be fully qualified.
    private fun resolve(name: String, reference: String): PreviewTheme? {
        val qualifiedName = reference.substringBefore('(').trim()
        val arguments = parseArguments(reference)
        if (qualifiedName.isEmpty() || arguments == null) {
            if (qualifiedName.isEmpty()) logger.error("Arkive: theme \"$name\" names no function.")
            return null
        }

        val candidates = resolver
            .getFunctionDeclarationsByName(resolver.getKSNameFromString(qualifiedName), includeTopLevel = true)
            .filter { it.isComposable() }
            .toList()
        val theme = candidates.firstNotNullOfOrNull { it.toPreviewTheme(name, arguments) }

        when {
            candidates.isEmpty() -> logger.error(
                "Arkive: previewTheme \"$qualifiedName\" is not a top-level @Composable function on the classpath.",
            )
            theme == null -> logger.error(
                "Arkive: previewTheme '$reference' must take a trailing @Composable content lambda, and " +
                    "every other parameter needs a default or an argument in the reference — except one " +
                    "optional Boolean parameter with 'dark' in its name, which the light/dark variants drive.",
            )
        }
        return theme
    }

    // Named arguments between the outermost parentheses, split at top-level commas.
    private fun parseArguments(reference: String): Map<String, String>? {
        val body = reference.substringAfter('(', missingDelimiterValue = "").trim()
        val arguments = splitTopLevel(body.removeSuffix(")")).map { it.trim() }.filter { it.isNotEmpty() }
        val unnamed = arguments.filterNot { it.matches(NAMED_ARGUMENT) }
        val problem = when {
            '(' in reference && !body.endsWith(')') -> "has unbalanced parentheses"
            unnamed.isNotEmpty() -> "needs named arguments (name = value), got $unnamed"
            else -> null
        }
        if (problem != null) {
            logger.error("Arkive: previewTheme '$reference' $problem.")
            return null
        }
        return arguments.associateBy { it.substringBefore('=').trim() }
    }

    private fun splitTopLevel(text: String): List<String> {
        val parts = mutableListOf<String>()
        var depth = 0
        var start = 0
        text.forEachIndexed { index, char ->
            when (char) {
                '(', '[', '{' -> depth++
                ')', ']', '}' -> depth--
                ',' -> if (depth == 0) {
                    parts += text.substring(start, index)
                    start = index + 1
                }
            }
        }
        return parts + text.substring(start)
    }

    private fun KSFunctionDeclaration.toPreviewTheme(name: String, arguments: Map<String, String>): PreviewTheme? {
        val content = parameters.lastOrNull()?.takeIf { it.type.resolve().isFunctionType }
        // A dark-mode flag the reference pins itself is not driven by the variants.
        val darkTheme = parameters
            .filter { it.isDarkThemeParameter() && it.name?.asString() !in arguments }
            .singleOrNull()
        val unset = parameters.filter { it != content && it != darkTheme && it.name?.asString() !in arguments }
        val parameterNames = parameters.mapNotNull { it.name?.asString() }
        if (content == null || !parameterNames.containsAll(arguments.keys) || unset.any { !it.hasDefault }) {
            return null
        }

        return PreviewTheme(
            name = name,
            member = MemberName(packageName.asString(), simpleName.asString()),
            arguments = arguments.values.toList(),
            darkThemeParameter = darkTheme?.name?.asString(),
            darkThemeRequired = darkTheme?.hasDefault == false,
        )
    }

    private fun KSValueParameter.isDarkThemeParameter(): Boolean =
        name?.asString().orEmpty().contains(DARK, ignoreCase = true) &&
            type.resolve().declaration.qualifiedName?.asString() == BOOLEAN

    private fun KSFunctionDeclaration.isComposable(): Boolean =
        annotations.any { it.annotationType.resolve().declaration.qualifiedName?.asString() == COMPOSABLE }

    private companion object {
        private const val COMPOSABLE = "androidx.compose.runtime.Composable"
        private const val BOOLEAN = "kotlin.Boolean"
        private const val DEFAULT_NAME = "default"
        private val SKIN_NAME = Regex("[A-Za-z0-9]+")
        private const val DARK = "dark"
        private val NAMED_ARGUMENT = Regex("""[A-Za-z_][A-Za-z0-9_]*\s*=[^=].*""", RegexOption.DOT_MATCHES_ALL)
    }
}

package com.infinum.arkive.composeutils.uitree

/** A call site from Compose source information; [lineNumber] as Compose records it. */
internal data class CallLocation(
    val sourceFile: String?,
    val lineNumber: Int,
    val packageHash: Int,
)

/** One slot-table group, abstracted from ui-tooling-data so attribution is testable on the JVM. */
internal interface SlotGroup {
    val name: String?
    val location: CallLocation?

    /** The emitted node (a LayoutNode for Compose UI) when this is a node group. */
    val node: Any?
    val children: Collection<SlotGroup>
}

internal data class Attribution(
    val composable: String?,
    val source: String?,
    val callers: List<String>,
)

/**
 * Names every emitted node from the groups between it and the nearest emitted ancestor:
 * the innermost call made from app code is the node's `composable` (its call site is the
 * `source`), the outer app calls are its `callers`. A node with no app call keeps the
 * innermost named group and no source — library internals. Compiler-synthetic groups
 * (names like `<get-typography>`) are treated as unnamed throughout.
 */
internal object ComposableAttribution {

    // Set from the Phase 0 probe: Compose already records 1-based lines, like an IDE.
    private const val LINE_NUMBER_BASE = 0

    fun attribute(roots: List<SlotGroup>, appPackages: AppPackages): Map<Any, Attribution> {
        val visitor = Visitor(appPackages)
        roots.forEach(visitor::visit)
        return visitor.result
    }

    fun pick(chain: List<SlotGroup>, appPackages: AppPackages): Attribution {
        val appCalls = chain.filter { group ->
            val location = group.location
            group.realName() != null && location != null && location.packageHash in appPackages
        }
        val innermost = appCalls.lastOrNull()
        return if (innermost != null) {
            Attribution(
                composable = innermost.realName(),
                source = innermost.location?.toSource(),
                callers = appCalls.dropLast(1).mapNotNull { it.realName() },
            )
        } else {
            Attribution(composable = chain.asReversed().firstNotNullOfOrNull { it.realName() }, source = null, callers = emptyList())
        }
    }

    private fun SlotGroup.realName(): String? = name?.takeUnless { it.startsWith("<") }

    private fun CallLocation.toSource(): String? = sourceFile?.let { "$it:${lineNumber + LINE_NUMBER_BASE}" }

    private class Visitor(private val appPackages: AppPackages) {
        val result = LinkedHashMap<Any, Attribution>()
        private val chain = ArrayList<SlotGroup>()

        fun visit(group: SlotGroup) {
            chain.add(group)
            val node = group.node
            if (node == null) {
                group.children.forEach(::visit)
            } else {
                result.putIfAbsent(node, pick(chain, appPackages))
                val outer = ArrayList(chain)
                chain.clear()
                group.children.forEach(::visit)
                chain.clear()
                chain.addAll(outer)
            }
            chain.removeAt(chain.lastIndex)
        }
    }
}

package com.infinum.arkive.composeutils.uitree

/**
 * Rebuilds the layout-node tree under a root from the nodes the slot tables reported. A node
 * whose direct parent was never reported hangs off its nearest reported ancestor. Nodes that
 * never reach the root — other windows such as dialogs — are counted in
 * [Assembly.unreachable]; ancestors of the root (the engine's own host nodes) are not.
 */
internal class TreeAssembler<N : Any>(private val parentOf: (N) -> N?) {

    class Assembly<N : Any>(
        private val children: Map<N, List<N>>,
        val unreachable: Int,
    ) {
        fun childrenOf(node: N): List<N> = children[node].orEmpty()
    }

    fun assemble(root: N, nodes: List<N>): Assembly<N> {
        val indexed = LinkedHashSet(nodes).apply { add(root) }
        val rootAncestors = generateSequence(parentOf(root), parentOf).toSet()
        val children = LinkedHashMap<N, MutableList<N>>()
        for (node in indexed) {
            if (node == root || node in rootAncestors) continue
            val parent = generateSequence(parentOf(node), parentOf).firstOrNull { it in indexed }
            if (parent != null) children.getOrPut(parent) { mutableListOf() }.add(node)
        }
        val excluded = 1 + rootAncestors.count { it in indexed }
        return Assembly(children, unreachable = indexed.size - excluded - countReachable(root, children))
    }

    private fun countReachable(root: N, children: Map<N, List<N>>): Int {
        var count = 0
        val stack = ArrayDeque(children[root].orEmpty())
        while (stack.isNotEmpty()) {
            val node = stack.removeLast()
            count++
            stack.addAll(children[node].orEmpty())
        }
        return count
    }
}

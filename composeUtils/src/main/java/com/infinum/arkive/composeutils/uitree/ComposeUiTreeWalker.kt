@file:OptIn(UiToolingDataApi::class)

package com.infinum.arkive.composeutils.uitree

import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.tooling.CompositionData
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.LayoutInfo
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.tooling.data.Group
import androidx.compose.ui.tooling.data.NodeGroup
import androidx.compose.ui.tooling.data.UiToolingDataApi
import androidx.compose.ui.tooling.data.asTree

/**
 * Joins three live sources on the frame being drawn — the layout-node tree (spine and
 * bounds), the unmerged semantics tree (text, roles, actions, state, text layout) and the
 * recorded slot tables (composable names and call sites) — via layout-node identity.
 */
internal class ComposeUiTreeWalker(
    private val shotId: String,
    private val view: View,
    private val tables: Set<CompositionData>,
    private val appPackages: AppPackages,
    private val log: (String) -> Unit = ::println,
) {
    private var reportedOtherWindows = false

    fun walk(): UiTree {
        val rootForTest = view as? ViewRootForTest
            ?: error("LocalView is ${view.javaClass.name}, not a ViewRootForTest")
        val semantics = indexSemantics(rootForTest.semanticsOwner.unmergedRootSemanticsNode)
        val captureRoot = semantics.values.firstOrNull { CaptureRootKey in it.config }
            ?: error("the capture root is missing from the semantics tree")
        val rootInfo = captureRoot.layoutInfo
        val slotRoots = tables.mapNotNull { data -> runCatching { ToolingSlotGroup(data.asTree()) }.getOrNull() }
        val attributions = ComposableAttribution.attribute(slotRoots, appPackages)
        val layoutNodes = attributions.keys.filterIsInstance<LayoutInfo>().filter { it.isAttached && it.isPlaced }
        val assembly = TreeAssembler<LayoutInfo> { it.parentInfo }.assemble(rootInfo, layoutNodes)
        reportOtherWindows(assembly.unreachable)

        val contentNodes = assembly.childrenOf(rootInfo)
        val density = (contentNodes.firstOrNull() ?: rootInfo).density.density
        val builder = NodeBuilder(rootInfo.coordinates, density, semantics, attributions, assembly)
        val root = contentNodes.singleOrNull()?.let(builder::build)
            ?: builder.build(rootInfo).copy(composable = null, callers = emptyList(), source = null)
        return UiTree(
            id = shotId,
            density = density,
            width = rootInfo.width / density,
            height = rootInfo.height / density,
            root = UiTreePruning.prune(attachHostedViews(root, rootInfo, density)),
        )
    }

    private fun reportOtherWindows(count: Int) {
        if (count > 0 && !reportedOtherWindows) {
            reportedOtherWindows = true
            log("Arkive: ui tree for $shotId covers the captured window only — $count node(s) elsewhere (e.g. dialogs) skipped")
        }
    }

    // AndroidView holders sit under AndroidComposeView inside compose-internal containers
    // (androidx.compose.ui.*); the first non-internal view on each path is app content. Views
    // directly under AndroidComposeView are its own plumbing (the 1x1 frame-rate view), never hosted.
    private fun attachHostedViews(root: UiTreeNode, rootInfo: LayoutInfo, density: Float): UiTreeNode {
        val composeView = view as? ViewGroup ?: return root
        val origin = rootInfo.coordinates.positionInRoot()
        val hosted = mutableListOf<UiTreeNode>()
        collectHostedViews(composeView, 0f, 0f) { hostedView, x, y ->
            hosted += ViewTreeBuilder.node(hostedView, x - origin.x, y - origin.y, density)
        }
        return hosted.fold(root, UiTreeInterop::attach)
    }

    private fun collectHostedViews(parent: ViewGroup, x: Float, y: Float, onFound: (View, Float, Float) -> Unit) {
        for (index in 0 until parent.childCount) {
            val child = parent.getChildAt(index)
            val childX = x + child.left - parent.scrollX + child.translationX
            val childY = y + child.top - parent.scrollY + child.translationY
            if (child.javaClass.name.startsWith(COMPOSE_INTERNAL_PREFIX) && child is ViewGroup) {
                // Recursed only to find hosted Views; a ComposeView inside a hosted View is
                // emitted as View nodes (ViewTreeBuilder), not walked as a nested compose tree.
                collectHostedViews(child, childX, childY, onFound)
            } else if (parent !== view && !child.javaClass.name.startsWith(COMPOSE_INTERNAL_PREFIX)) {
                onFound(child, childX, childY)
            }
        }
    }

    private companion object {
        const val COMPOSE_INTERNAL_PREFIX = "androidx.compose.ui."
    }
}

private class NodeBuilder(
    private val rootCoordinates: LayoutCoordinates,
    private val density: Float,
    private val semantics: Map<Int, SemanticsNode>,
    private val attributions: Map<Any, Attribution>,
    private val assembly: TreeAssembler.Assembly<LayoutInfo>,
) {
    fun build(info: LayoutInfo): UiTreeNode {
        val attribution = attributions[info]
        val semanticsNode = semantics[info.semanticsId]
        val snapshot = semanticsNode?.let(::semanticsSnapshot)
        val actions = snapshot?.let { SemanticsMapping.actions(it.keyNames) }.orEmpty()
        return UiTreeNode(
            bounds = bounds(info),
            composable = attribution?.composable,
            callers = attribution?.callers.orEmpty(),
            source = attribution?.source,
            role = snapshot?.role,
            text = snapshot?.text,
            editableText = snapshot?.editableText,
            contentDescription = snapshot?.contentDescription,
            testTag = snapshot?.testTag,
            actions = actions,
            state = snapshot?.let { SemanticsMapping.state(it, actions) },
            textStyle = semanticsNode?.let(::textStyleSnapshot)?.let(SemanticsMapping::textStyle),
            children = assembly.childrenOf(info).map(::build),
        )
    }

    // LayoutInfo.coordinates is the node's inner coordinator, which excludes its own modifiers
    // (padding, background); the head modifier sits on the outer one — the box the snapshot shows.
    // localBoundingBoxOf collapses an empty rect to Rect.Zero, so zero-area nodes are placed by origin.
    private fun bounds(info: LayoutInfo): UiBounds {
        val outer = info.getModifierInfo().firstOrNull()?.coordinates ?: info.coordinates
        val width = outer.size.width.toFloat()
        val height = outer.size.height.toFloat()
        val rect = if (width > 0f && height > 0f) {
            rootCoordinates.localBoundingBoxOf(outer, clipBounds = false)
        } else {
            Rect(rootCoordinates.localPositionOf(outer, Offset.Zero), Size(width, height))
        }
        return UiBounds(rect.left / density, rect.top / density, rect.width / density, rect.height / density)
    }
}

private class ToolingSlotGroup(private val group: Group) : SlotGroup {
    override val name: String? get() = group.name
    override val location: CallLocation?
        get() = group.location?.let { CallLocation(it.sourceFile, it.lineNumber, it.packageHash) }
    override val node: Any? get() = (group as? NodeGroup)?.node
    override val children: Collection<SlotGroup> get() = group.children.map(::ToolingSlotGroup)
}

private fun indexSemantics(root: SemanticsNode): Map<Int, SemanticsNode> {
    val index = HashMap<Int, SemanticsNode>()
    val stack = ArrayDeque(listOf(root))
    while (stack.isNotEmpty()) {
        val node = stack.removeLast()
        index[node.id] = node
        stack.addAll(node.children)
    }
    return index
}

private fun semanticsSnapshot(node: SemanticsNode): SemanticsSnapshot {
    val config = node.config
    return SemanticsSnapshot(
        role = config.getOrNull(SemanticsProperties.Role)?.toString(),
        text = config.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { it.text }?.takeIf { it.isNotEmpty() },
        editableText = config.getOrNull(SemanticsProperties.EditableText)?.text,
        contentDescription = config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(" ")?.takeIf { it.isNotEmpty() },
        testTag = config.getOrNull(SemanticsProperties.TestTag),
        keyNames = config.map { it.key.name },
        disabled = SemanticsProperties.Disabled in config,
        selected = config.getOrNull(SemanticsProperties.Selected),
        toggle = config.getOrNull(SemanticsProperties.ToggleableState)?.name,
        focused = config.getOrNull(SemanticsProperties.Focused),
    )
}

private fun textStyleSnapshot(node: SemanticsNode): TextStyleSnapshot? {
    val results = mutableListOf<TextLayoutResult>()
    val laidOut = node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results) == true
    return results.firstOrNull()?.takeIf { laidOut }?.let { result ->
        val style = result.layoutInput.style
        TextStyleSnapshot(
            fontSizeSp = style.fontSize.takeIf { it.isSp }?.value,
            fontWeight = style.fontWeight?.weight,
            lineHeightSp = style.lineHeight.takeIf { it.isSp }?.value,
            colorArgb = style.color.takeIf { it.isSpecified }?.toArgb(),
            maxLines = result.layoutInput.maxLines,
            // Not hasVisualOverflow: the Phase 0 probe found it unreliable across engines.
            overflow = result.multiParagraph.didExceedMaxLines ||
                (0 until result.lineCount).any { result.isLineEllipsized(it) },
        )
    }
}

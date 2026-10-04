package com.infinum.arkive.composeutils.uitree

import org.junit.Assert.assertEquals
import org.junit.Test

class TreeAssemblerTest {

    private class Fake(val name: String, val parent: Fake?) {
        override fun toString() = name
    }

    private val assembler = TreeAssembler<Fake> { it.parent }

    @Test
    fun unreportedIntermediateNodesAreSkipped() {
        val root = Fake("root", null)
        val a = Fake("a", root)
        val hidden = Fake("hidden", a)
        val b = Fake("b", hidden)

        val assembly = assembler.assemble(root, listOf(a, b))

        assertEquals(listOf(a), assembly.childrenOf(root))
        assertEquals(listOf(b), assembly.childrenOf(a))
        assertEquals(0, assembly.unreachable)
    }

    @Test
    fun childOrderFollowsReportOrder() {
        val root = Fake("root", null)
        val first = Fake("first", root)
        val second = Fake("second", root)

        assertEquals(listOf(first, second), assembler.assemble(root, listOf(first, second)).childrenOf(root))
    }

    @Test
    fun nodesInOtherWindowsAreCountedNotAttached() {
        val root = Fake("root", null)
        val child = Fake("child", root)
        val dialogWindow = Fake("dialog", null)
        val dialogText = Fake("dialogText", dialogWindow)

        val assembly = assembler.assemble(root, listOf(child, dialogWindow, dialogText))

        assertEquals(listOf(child), assembly.childrenOf(root))
        assertEquals(2, assembly.unreachable)
    }

    @Test
    fun ancestorsOfTheRootAreNotCountedAsUnreachable() {
        val host = Fake("host", null)
        val root = Fake("root", host)
        val child = Fake("child", root)

        val assembly = assembler.assemble(root, listOf(host, root, child))

        assertEquals(listOf(child), assembly.childrenOf(root))
        assertEquals(0, assembly.unreachable)
    }
}

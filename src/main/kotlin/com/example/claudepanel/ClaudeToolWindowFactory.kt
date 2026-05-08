package com.example.claudepanel

import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import org.jetbrains.plugins.terminal.LocalTerminalDirectRunner
import org.jetbrains.plugins.terminal.ShellTerminalWidget
import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent

class ClaudeToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val widget = LocalTerminalDirectRunner.createTerminalRunner(project)
            .createTerminalWidget(toolWindow.disposable, project.basePath, true)

        val content = ContentFactory.getInstance()
            .createContent(widget.component, "", false)
        content.isCloseable = false
        toolWindow.contentManager.addContent(content)

        val shell = widget as? ShellTerminalWidget
        shell?.executeCommand(ClaudePanelSettings.getInstance().command)
        if (shell != null) installKeyDispatcher(shell, toolWindow.disposable)
    }

    private fun installKeyDispatcher(widget: ShellTerminalWidget, parent: Disposable) {
        val dispatcher = IdeEventQueue.EventDispatcher { event ->
            if (event !is KeyEvent || event.id != KeyEvent.KEY_PRESSED) return@EventDispatcher false
            val focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner ?: return@EventDispatcher false
            val panel = widget.terminalPanel
            if (focus !== panel) return@EventDispatcher false
            val starter = widget.terminalStarter ?: return@EventDispatcher false

            val bytes: ByteArray? = when {
                event.keyCode == KeyEvent.VK_ESCAPE && event.modifiersEx == 0 ->
                    byteArrayOf(0x1b)
                event.keyCode == KeyEvent.VK_ENTER && event.modifiersEx == KeyEvent.SHIFT_DOWN_MASK ->
                    "[27;2;13~".toByteArray()
                else -> null
            }
            if (bytes == null) return@EventDispatcher false
            starter.sendBytes(bytes, false)
            event.consume()
            true
        }
        IdeEventQueue.getInstance().addDispatcher(dispatcher, parent)
    }
}

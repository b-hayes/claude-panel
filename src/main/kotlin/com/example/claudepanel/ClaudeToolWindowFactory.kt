package com.example.claudepanel

import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import org.jetbrains.plugins.terminal.LocalTerminalDirectRunner
import org.jetbrains.plugins.terminal.ShellTerminalWidget
import java.awt.BorderLayout
import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.SwingUtilities

class ClaudeToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        Messages.showInfoMessage(project, "VERSION 0.1.1 - PLUGIN LOADING", "DEBUG")
        val widget = LocalTerminalDirectRunner.createTerminalRunner(project)
            .createTerminalWidget(toolWindow.disposable, project.basePath, true)

        val shell = widget as? ShellTerminalWidget

        val panel = JPanel(BorderLayout())
        val menuButton = JButton("Menu")
        menuButton.addActionListener {
            showAgentSelectionDialog(project, shell)
        }
        panel.add(menuButton, BorderLayout.NORTH)
        panel.add(widget.component, BorderLayout.CENTER)

        val content = ContentFactory.getInstance()
            .createContent(panel, "", false)
        content.isCloseable = false
        toolWindow.contentManager.addContent(content)

        if (shell != null) {
            installKeyDispatcher(shell, toolWindow.disposable)
            // Show dialog on initial open
            SwingUtilities.invokeLater {
                showAgentSelectionDialog(project, shell)
            }
        }
    }

    private fun showAgentSelectionDialog(project: Project, shell: ShellTerminalWidget?) {
        println("DEBUG: showAgentSelectionDialog called")
        val dialog = CommandSelectionDialog(project)
        if (dialog.showAndGet()) {
            val selected = dialog.getSelectedOption()
            if (selected != null) {
                println("DEBUG: Selected command: $selected")
                shell?.executeCommand(selected)
            } else {
                println("DEBUG: Selection was Cancel or nothing")
            }
        } else {
            println("DEBUG: Dialog was closed without OK")
        }
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

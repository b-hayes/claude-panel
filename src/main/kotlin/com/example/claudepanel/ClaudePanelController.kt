package com.example.claudepanel

import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.components.JBLabel
import com.jediterm.terminal.ui.TerminalWidget
import com.jediterm.terminal.ui.TerminalWidgetListener
import org.jetbrains.plugins.terminal.LocalTerminalDirectRunner
import org.jetbrains.plugins.terminal.ShellTerminalWidget
import java.awt.CardLayout
import java.awt.BorderLayout
import java.awt.Component
import java.awt.KeyboardFocusManager
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.Timer
import javax.swing.SwingUtilities.invokeLater

/**
 * Content for the Claude tool window.
 */
class ClaudePanelController(
    private val project: Project,
    private val toolWindow: ToolWindow
) : JPanel(CardLayout()) {

    private val card = CardLayout()
    private var currentWidget: ShellTerminalWidget? = null
    private var currentSession: Disposable? = null

    init {
        layout = card
        add(buildMenuPanel(), CARD_MENU)
    }

    private fun buildMenuPanel(): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.isOpaque = false

        val intro = JBLabel(
            """
            <html>
            <p style='margin:6px 0 0 0'>
                I wanted the Claude code plugin without it loosing my terminal! <br>
                Use this side terminal just for your AI sessions and keep your main window free for the commands you actually run.
            </p>
            </html>
            """.trimIndent()
        )
        intro.horizontalAlignment = SwingConstants.LEFT
        panel.add(intro)
        panel.add(Box.createVerticalStrut(16))

        val title = JBLabel("Choose a command to run:")
        title.horizontalAlignment = SwingConstants.CENTER
        panel.add(title)
        panel.add(Box.createVerticalStrut(12))

        // Create dropdown panel inline and wire up callback immediately
        val dropdownPanel = DropdownPanel().apply {
            setOnChange { provider, model ->
                val config = when (provider.lowercase()) {
                    "default" -> LaunchConfig("claude --model $model", runCommand = false)
                    "ollama" -> {
                        val ollamaCmd = if (model == "N/A - command not found") "" else "ollama launch $model"
                        LaunchConfig(ollamaCmd, model.isNotBlank() && !model.startsWith("N/A"))
                    }
                    "openrouter" -> {
                        val modelStr = provider
                        val command = """ANTHROPIC_BASE_URL=https://openrouter.ai/api/v1 \
                            ANTHROPIC_AUTH_TOKEN=openrouter \
                            AGENT_BACKEND=openrouter \
                            CLAUDE_CODE_MAX_OUTPUT_TOKENS=16000 \
                            CLAUDE_CODE_DISABLE_BUNDLED_SKILLS=1 \
                            claude --model $modelStr"""
                        LaunchConfig(command, runCommand = true)
                    }
                    else -> LaunchConfig("", runCommand = false)
                }

                ApplicationManager.getApplication().invokeLater {
                    showTerminal(config)
                }
            }
        }

        // Add a separator and hint text
        panel.add(Box.createVerticalGlue())
        val hint = JBLabel("<html><center>The panel returns to this menu<br>when the process exits.</center></html>")
        hint.foreground = hint.foreground?.let { java.awt.Color(it.red, it.green, it.blue, 128) }
        hint.horizontalAlignment = SwingConstants.CENTER
        panel.add(hint)

        return panel
    }

    private fun launch(command: String) {
        if (project.isDisposed) return
        if (command.isBlank()) {
            showTerminal(LaunchConfig(command, runCommand = false))
            return
        }
        showTerminal(LaunchConfig(command, runCommand = true))
    }

    private data class LaunchConfig(val command: String, val runCommand: Boolean)

    private fun showTerminal(launch: LaunchConfig) {
        if (currentWidget != null) return // a session is already active
        val runner = LocalTerminalDirectRunner.createTerminalRunner(project)

        val session = Disposer.newDisposable("claude-panel-session")
        Disposer.register(toolWindow.disposable, session)

        val widget = try {
            runner.createTerminalWidget(session, project.basePath, true) as? ShellTerminalWidget
        } catch (e: Exception) {
            LOG.warn("Failed to create terminal widget for launch '${launch.command}'", e)
            Disposer.dispose(session)
            return
        } ?: run {
            Disposer.dispose(session)
            return
        }
        currentWidget = widget
        currentSession = session

        widget.addListener(object : TerminalWidgetListener {
            override fun allSessionsClosed(terminalWidget: TerminalWidget) {
                ApplicationManager.getApplication().invokeLater {
                    if (currentWidget === widget) onSessionEnded()
                }
            }
        })

        installKeyDispatcher(widget, session)
        add(widget.component, CARD_TERMINAL)
        card.show(this, CARD_TERMINAL)
        revalidate()
        repaint()

        // Give the shell a moment to become ready, then optionally send the command.
        if (launch.runCommand) {
            val timer = javax.swing.Timer(300, object : ActionListener {
                override fun actionPerformed(e: ActionEvent?) {
                    try {
                        widget.executeCommand(launch.command)
                    } catch (ex: Exception) {
                        LOG.warn("Failed to execute '${launch.command}'", ex)
                    }
                }
            })
            timer.isRepeats = false
            timer.start()
        }
    }

    private fun onSessionEnded() {
        currentWidget?.let { remove(it.component) }
        currentWidget = null
        currentSession?.let { Disposer.dispose(it) }
        currentSession = null
        card.show(this, CARD_MENU)
        revalidate()
        repaint()
    }

    /**
     * Keeps the Escape (submit) and Shift+Enter (new line) key bindings that
     * the agent CLIs rely on, forwarding them raw to the terminal.
     */
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
                    "[27;2;13~".toByteArray()
                else -> null
            }
            if (bytes == null) return@EventDispatcher false
            starter.sendBytes(bytes, false)
            event.consume()
            true
        }
        IdeEventQueue.getInstance().addDispatcher(dispatcher, parent)
    }

    companion object {
        private val LOG = Logger.getInstance(ClaudePanelController::class.java)

        private const val CARD_MENU = "menu"
        private const val CARD_TERMINAL = "terminal"

        // These are no longer used since we're using the dropdown panel instead
        private val AGENT_COMMANDS = listOf("default")
    }
}

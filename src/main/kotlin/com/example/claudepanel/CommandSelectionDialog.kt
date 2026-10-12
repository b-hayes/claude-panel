package com.example.claudepanel

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBList
import java.awt.BorderLayout
import javax.swing.*

class CommandSelectionDialog(project: Project) : DialogWrapper(project) {
    private val options = arrayOf("claude", "ollama launch pi", "Cancel")
    private val list = JBList(options)

    init {
        title = "Claude Panel"
        init()
    }

    override fun createCenterPanel(): JComponent {
        return JPanel(BorderLayout()).apply {
            add(list, BorderLayout.CENTER)
            val buttonPanel = JPanel()
            val okButton = JButton("OK")
            okButton.addActionListener {
                close(DialogWrapper.OK_EXIT_CODE)
            }
            buttonPanel.add(okButton)
            add(buttonPanel, BorderLayout.SOUTH)
        }
    }

    fun getSelectedOption(): String? {
        val index = list.selectedIndex
        if (index == -1) return null
        val selected = options[index]
        return if (selected == "Cancel") null else selected
    }
}

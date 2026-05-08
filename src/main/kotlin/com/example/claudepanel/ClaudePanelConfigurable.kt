package com.example.claudepanel

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

class ClaudePanelConfigurable : Configurable {
    private val field = JBTextField()
    private var panel: JPanel? = null

    override fun getDisplayName(): String = "Claude Panel"

    override fun createComponent(): JComponent {
        val p = FormBuilder.createFormBuilder()
            .addLabeledComponent("Startup command:", field, 1, false)
            .addComponentFillVertically(JPanel(), 0)
            .panel
        panel = p
        return p
    }

    override fun isModified(): Boolean = field.text != ClaudePanelSettings.getInstance().command

    override fun apply() {
        ClaudePanelSettings.getInstance().command = field.text
    }

    override fun reset() {
        field.text = ClaudePanelSettings.getInstance().command
    }
}

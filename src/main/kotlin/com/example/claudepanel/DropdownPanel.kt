package com.example.claudepanel

import javax.swing.*
import java.awt.FlowLayout

/**
 * A compact dropdown selector for choosing a provider and model combo.
 * Displays as two adjacent ComboBoxes: (Provider) | (Model).
 */
class DropdownPanel : JPanel() {

    private var onSelectedChanged: ((provider: String, model: String) -> Unit)? = null
    private val providerBox = JComboBox<String>()
    private val modelBox = JComboBox<String>()
    private var repopulating = false

    init {
        layout = FlowLayout(FlowLayout.LEFT)
        isOpaque = false

        providerBox.addItemsToModel("default", "ollama", "openrouter")
        providerBox.isEditable = false
        providerBox.addActionListener { _ -> updateModelDropdown() }

        modelBox.isEditable = false
        modelBox.addActionListener { _ ->
            if (repopulating) return@addActionListener
            val provider = (providerBox.selectedItem as? String) ?: return@addActionListener
            val model = (modelBox.selectedItem as? String) ?: return@addActionListener
            onSelectedChanged?.invoke(provider, model)
        }

        updateModelDropdown()

        val separator = object : JPanel() {
            init {
                layout = FlowLayout(FlowLayout.CENTER)
                add(Box.createHorizontalStrut(12))
                add(JLabel(" | "))
                add(Box.createHorizontalStrut(12))
            }
        }

        add(providerBox)
        add(separator)
        add(modelBox)
    }

    private fun updateModelDropdown() {
        val provider = (providerBox.selectedItem as? String) ?: "default"

        repopulating = true
        modelBox.removeAllItems()
        when (provider.lowercase()) {
            "default" -> modelBox.addItem("default")
            "ollama" -> {
                if (isOllamaAvailable()) {
                    listOf("qwen3.5:9b", "qwen2.5-coder:14b", "glm-4.7-flash:latest")
                        .forEach { modelBox.addItem(it) }
                } else {
                    modelBox.addItem("N/A - command not found")
                }
            }
            "openrouter" -> listOf("z-ai/glm-5", "z-ai/glm-5.3-flash", "cohere/north-mini-code:free")
                .forEach { modelBox.addItem(it) }
        }
        repopulating = false
    }

    private fun isOllamaAvailable(): Boolean {
        return try {
            // Check if ollama command exists
            Runtime.getRuntime().exec("ollama")
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun <T : Comparable<T>> JComboBox<T>.addItemsToModel(vararg items: T) {
        removeAllItems()
        items.forEach { addItem(it) }
    }

    fun setOnChange(listener: ((provider: String, model: String) -> Unit)) {
        onSelectedChanged = listener
    }
}

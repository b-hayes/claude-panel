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

    init {
        layout = FlowLayout(FlowLayout.LEFT)
        isOpaque = false

        // Configure provider dropdown
        providerBox.addItemsToModel("default", "ollama", "openrouter")
        providerBox.isEditable = false
        providerBox.addActionListener { _ -> updateModelDropdown() }

        // Configure model dropdown - add default item initially
        modelBox.isEditable = false

        val separator = object : JPanel() {
            init {
                layout = FlowLayout(FlowLayout.CENTER)
                add(Box.createHorizontalStrut(12))
                add(JLabel(" | "))
                add(Box.createHorizontalStrut(12))
            }
        }

        add(providerBox, 0)
        add(separator, 0)
        add(modelBox, 0)
    }

    private fun updateModelDropdown() {
        val provider = (providerBox.selectedItem as? String) ?: "default"

        when (provider.lowercase()) {
            "default" -> {
                modelBox.removeAllItems()
                modelBox.addItem("default")
            }
            "ollama" -> {
                if (!isOllamaAvailable()) {
                    modelBox.removeAllItems()
                    modelBox.addItem("N/A - command not found")
                    return
                }
                modelBox.removeAllItems()

                // Add known models for Ollama
                modelBox.addItem("qwen3.5:9b")
                modelBox.addItem("qwen2.5-coder:14b")
                modelBox.addItem("glm-4.7-flash:latest")
            }
            "openrouter" -> {
                val models = listOf(
                    "z-ai/glm-5",
                    "z-ai/glm-5.3-flash",
                    "cohere/north-mini-code:free"
                )
                modelBox.removeAllItems()
                models.forEach { modelBox.addItem(it) }
            }
        }
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

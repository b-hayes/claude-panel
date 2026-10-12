package com.example.claudepanel

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class ClaudeToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val content = ContentFactory.getInstance()
            .createContent(ClaudePanelController(project, toolWindow), "", false)
        content.isCloseable = false
        toolWindow.contentManager.addContent(content)
    }
}

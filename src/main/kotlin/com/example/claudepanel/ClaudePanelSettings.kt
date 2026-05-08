package com.example.claudepanel

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

@Service(Service.Level.APP)
@State(name = "ClaudePanelSettings", storages = [Storage("claudePanel.xml")])
class ClaudePanelSettings : PersistentStateComponent<ClaudePanelSettings.State> {
    data class State(var command: String = "claude")

    private var state = State()

    override fun getState(): State = state
    override fun loadState(s: State) { state = s }

    var command: String
        get() = state.command
        set(value) { state.command = value }

    companion object {
        fun getInstance(): ClaudePanelSettings = service()
    }
}

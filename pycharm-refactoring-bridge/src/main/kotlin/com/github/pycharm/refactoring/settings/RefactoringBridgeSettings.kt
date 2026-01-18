package com.github.pycharm.refactoring.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "RefactoringBridgeSettings",
    storages = [Storage("RefactoringBridgeSettings.xml")]
)
class RefactoringBridgeSettings : PersistentStateComponent<RefactoringBridgeSettings> {

    var port: Int = 9876
    var enabled: Boolean = true
    var authToken: String = ""
    var allowedProjectPaths: MutableList<String> = mutableListOf()

    override fun getState(): RefactoringBridgeSettings = this

    override fun loadState(state: RefactoringBridgeSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        fun getInstance(): RefactoringBridgeSettings =
            ApplicationManager.getApplication().getService(RefactoringBridgeSettings::class.java)
    }
}

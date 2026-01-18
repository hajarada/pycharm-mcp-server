package com.github.pycharm.refactoring.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

class RefactoringBridgeConfigurable : Configurable {

    private var mainPanel: JPanel? = null
    private var portField: JBTextField? = null
    private var enabledCheckbox: JBCheckBox? = null
    private var authTokenField: JBTextField? = null

    override fun getDisplayName(): String = "Refactoring Bridge"

    override fun createComponent(): JComponent {
        val settings = RefactoringBridgeSettings.getInstance()

        portField = JBTextField(settings.port.toString(), 10)
        enabledCheckbox = JBCheckBox("Enable HTTP server", settings.enabled)
        authTokenField = JBTextField(settings.authToken, 30)

        mainPanel = FormBuilder.createFormBuilder()
            .addComponent(enabledCheckbox!!)
            .addLabeledComponent(JBLabel("Port:"), portField!!, 1, false)
            .addLabeledComponent(JBLabel("Auth Token (optional):"), authTokenField!!, 1, false)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        return mainPanel!!
    }

    override fun isModified(): Boolean {
        val settings = RefactoringBridgeSettings.getInstance()
        return portField?.text?.toIntOrNull() != settings.port ||
                enabledCheckbox?.isSelected != settings.enabled ||
                authTokenField?.text != settings.authToken
    }

    override fun apply() {
        val settings = RefactoringBridgeSettings.getInstance()
        settings.port = portField?.text?.toIntOrNull() ?: 9876
        settings.enabled = enabledCheckbox?.isSelected ?: true
        settings.authToken = authTokenField?.text ?: ""
    }

    override fun reset() {
        val settings = RefactoringBridgeSettings.getInstance()
        portField?.text = settings.port.toString()
        enabledCheckbox?.isSelected = settings.enabled
        authTokenField?.text = settings.authToken
    }

    override fun disposeUIResources() {
        mainPanel = null
        portField = null
        enabledCheckbox = null
        authTokenField = null
    }
}

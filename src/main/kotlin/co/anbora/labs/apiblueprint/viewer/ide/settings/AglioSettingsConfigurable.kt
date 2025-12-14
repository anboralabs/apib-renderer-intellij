package co.anbora.labs.apiblueprint.viewer.ide.settings

import co.anbora.labs.apiblueprint.viewer.ide.toolchain.AglioLocalToolchain
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import javax.swing.JComponent
import co.anbora.labs.apiblueprint.viewer.ide.toolchain.AglioToolchainService.Companion.toolchainSettings
import co.anbora.labs.apiblueprint.viewer.ide.utils.toPath
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.ShowSettingsUtil

class AglioSettingsConfigurable(private val project: Project) : Configurable {

    private val mainPanel: DialogPanel
    private val model = AglioProjectSettingsForm.Model(
        homeLocation = "",
        version = ""
    )
    private val settingsForm = AglioProjectSettingsForm(project, model)

    init {
        mainPanel = settingsForm.createComponent()
    }

    override fun getDisplayName() = "API Blueprint"

    override fun createComponent(): JComponent = mainPanel

    override fun getPreferredFocusedComponent(): JComponent = mainPanel

    override fun isModified(): Boolean {
        mainPanel.apply()

        val settings = toolchainSettings
        return model.homeLocation != settings.toolchain().homePath()
    }

    override fun apply() {
        mainPanel.apply()

        validateSettings()

        val settings = toolchainSettings
        settings.setToolchain(AglioLocalToolchain(model.version, model.homeLocation.toPath()))
    }

    private fun validateSettings() {
        val issues = mainPanel.validateAll()
        if (issues.isNotEmpty()) {
            throw ConfigurationException(issues.first().message)
        }
    }

    override fun reset() {
        val settings = toolchainSettings

        with(model) {
            homeLocation = settings.toolchain().homePath()
            version = settings.toolchain().version()
        }

        settingsForm.reset()
        mainPanel.reset()
    }

    companion object {
        @JvmStatic
        fun show(project: Project) {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, AglioSettingsConfigurable::class.java)
        }
    }
}
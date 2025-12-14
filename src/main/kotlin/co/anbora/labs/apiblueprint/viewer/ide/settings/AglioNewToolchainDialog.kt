package co.anbora.labs.apiblueprint.viewer.ide.settings

import co.anbora.labs.apiblueprint.viewer.ide.toolchain.AglioKnownToolchainsState
import co.anbora.labs.apiblueprint.viewer.ide.toolchain.AglioToolchain
import co.anbora.labs.apiblueprint.viewer.ide.toolchain.AglioToolchainFlavor
import co.anbora.labs.apiblueprint.viewer.ide.utils.toPath
import co.anbora.labs.apiblueprint.viewer.ide.utils.toPathOrNull
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.Disposer
import com.intellij.ui.JBColor
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.layout.ValidationInfoBuilder
import com.intellij.util.ui.JBDimension
import java.nio.file.Path
import javax.swing.JComponent
import javax.swing.JLabel
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

class AglioNewToolchainDialog(private val toolchainFilter: Condition<Path>, project: Project?) : DialogWrapper(project) {
    data class Model(
        var toolchainLocation: String,
        var toolchainVersion: String,
        var stdlibLocation: String,
    )

    private val model: Model = Model("", "N/A", "")
    private val mainPanel: DialogPanel
    private val toolchainVersion = JLabel()
    private val toolchainIconLabel = JLabel()
    private val pathToToolchainComboBox = AglioToolchainPathChoosingComboBox { onToolchainLocationChanged() }

    init {
        title = "New Preview"
        setOKButtonText("Add")

        mainPanel = panel {
            row("Location:") {
                cell(pathToToolchainComboBox)
                    .align(AlignX.FILL)
                    .validationOnApply { validateToolchainPath() }
            }
            row("Version:") {
                cell(toolchainVersion)
                    .bind(JLabel::getText, JLabel::setText, model::toolchainVersion.toMutableProperty())
                    .gap(RightGap.SMALL)
                    .apply {
                        component.foreground = JBColor.RED
                    }
                cell(toolchainIconLabel)
            }
            row("Binary:") {
                textField()
                    .align(AlignX.FILL)
                    .bindText(model::stdlibLocation)
                    .enabled(false)
            }
        }

        pathToToolchainComboBox.addToolchainsAsync {
            AglioToolchainFlavor.getApplicableFlavors().flatMap { it.suggestHomePaths() }.distinct().
                    filter { toolchainFilter.value(it) }
        }

        val disposable = Disposer.newDisposable()
        mainPanel.registerValidators(disposable)

        init()
    }

    override fun getPreferredFocusedComponent(): JComponent = pathToToolchainComboBox

    override fun createCenterPanel(): JComponent {
        return mainPanel.apply {
            preferredSize = JBDimension(450, height)
        }
    }

    override fun doOKAction() {
        if (AglioKnownToolchainsState.getInstance().isKnown(model.toolchainLocation)) {
            setErrorText("This location is already added")
            return
        }

        AglioKnownToolchainsState.getInstance().add(AglioToolchain.fromPath(model.toolchainLocation))

        super.doOKAction()
    }

    fun addedToolchain(): String? {
        return if (exitCode == OK_EXIT_CODE) model.toolchainLocation else null
    }

    private fun onToolchainLocationChanged() {
        model.toolchainLocation = pathToToolchainComboBox.selectedPath ?: ""

        model.toolchainVersion = ConfigurationUtil.guessToolchainVersion(model.toolchainLocation)

        if (model.toolchainVersion != ConfigurationUtil.UNDEFINED_VERSION) {
            val path = model.toolchainLocation.toPathOrNull()
            model.stdlibLocation = path?.resolve(ConfigurationUtil.STANDARD_AGLIO)?.toString() ?: ""
        } else {
            model.stdlibLocation = ""
        }

        mainPanel.reset()

        if (model.toolchainVersion == ConfigurationUtil.UNDEFINED_VERSION) {
            toolchainVersion.foreground = JBColor.RED
            toolchainIconLabel.icon = null
        } else {
            toolchainVersion.foreground = JBColor.foreground()
            toolchainIconLabel.icon = AllIcons.General.InspectionsOK
        }
    }

    private fun ValidationInfoBuilder.validateToolchainPath(): ValidationInfo? {
        if (model.toolchainLocation.isEmpty()) {
            return error("Aglio location is required")
        }

        val toolchainPath = model.toolchainLocation.toPath()
        if (!toolchainPath.exists()) {
            return error("Aglio location is invalid, $toolchainPath not exist")
        }

        if (!toolchainPath.isDirectory()) {
            return error("Aglio location must be a directory")
        }

        val version = ConfigurationUtil.guessToolchainVersion(model.toolchainLocation)
        if (version == ConfigurationUtil.UNDEFINED_VERSION) {
            return error("Aglio location is invalid, can't get version. Please check that folder contains ${ConfigurationUtil.STANDARD_AGLIO}")
        }

        return null
    }
}

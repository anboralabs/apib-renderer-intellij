package co.anbora.labs.apiblueprint.viewer.ide.settings

import co.anbora.labs.apiblueprint.viewer.ide.toolchain.AglioKnownToolchainsState
import co.anbora.labs.apiblueprint.viewer.ide.toolchain.AglioToolchain
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Condition
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import java.nio.file.Path

class AglioProjectSettingsForm(private val project: Project, private val model: Model) {

    data class Model(
        var homeLocation: String,
        var version: String
    )

    private val mainPanel: DialogPanel
    private val toolchainChooser = ToolchainChooserComponent(
        project,
        { showNewToolchainDialog() },
        { installNewToolchain() }
    ) { onSelect(it) }

    init {
        mainPanel = panel {
            row {
                cell(toolchainChooser)
                    .align(AlignX.FILL)
            }
        }

        val toolchain = toolchainChooser.selectedToolchain()

        // setup initial location
        model.homeLocation = toolchain?.homePath() ?: ""
        model.version = toolchain?.version() ?: ""
    }

    fun createComponent() = mainPanel

    fun reset() {
        toolchainChooser.select(model.homeLocation)
    }

    private fun installNewToolchain() {

    }

    private fun showNewToolchainDialog() {
        val dialog = AglioNewToolchainDialog(createFilterKnownToolchains(), project)
        if (!dialog.showAndGet()) {
            return
        }

        toolchainChooser.refresh()

        val addedToolchain = dialog.addedToolchain()
        if (addedToolchain != null) {
            toolchainChooser.select(addedToolchain)
        }
    }

    private fun createFilterKnownToolchains(): Condition<Path> {
        val knownToolchains = AglioKnownToolchainsState.getInstance().knownToolchains()
        return Condition { path ->
            knownToolchains.none { it.homePath() == path.toAbsolutePath().toString() }
        }
    }

    private fun onSelect(toolchainInfo: AglioToolchain) {
        model.homeLocation = toolchainInfo.homePath()
        model.version = toolchainInfo.version()
    }
}
package co.anbora.labs.apiblueprint.viewer.ide.settings

import co.anbora.labs.apiblueprint.viewer.ide.toolchain.AglioKnownToolchainsState
import co.anbora.labs.apiblueprint.viewer.ide.toolchain.AglioToolchain
import co.anbora.labs.apiblueprint.viewer.ide.toolchain.AglioToolchainService.Companion.toolchainSettings
import co.anbora.labs.apiblueprint.viewer.ide.toolchain.DownloadToolchain
import co.anbora.labs.apiblueprint.viewer.ide.toolchain.NullToolchain
import co.anbora.labs.apiblueprint.viewer.ide.ui.renderer.AglioListCellRenderer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ComponentWithBrowseButton
import com.intellij.util.ui.SwingHelper
import com.jgoodies.common.base.Objects
import java.awt.event.ActionListener
import java.awt.event.ItemEvent
import javax.swing.DefaultComboBoxModel

class ToolchainChooserComponent(
    private val project: Project,
    private val browseActionListener: ActionListener,
    private val downloadAction: Runnable,
    private val onSelectAction: (AglioToolchain) -> Unit
): ComponentWithBrowseButton<ComboBox<AglioToolchain>>(ComboBox<AglioToolchain>(), browseActionListener) {

    private val comboBox = childComponent
    private val knownToolchains get() = AglioKnownToolchainsState.getInstance().knownToolchains()
    private var knownToolchainInfos = AglioKnownToolchainsState.getInstance().knownToolchains()

    private var myLastSelectedItem: AglioToolchain = toolchainSettings.toolchain()
    private val myModel: ToolchainComboBoxModel = ToolchainComboBoxModel()


    init {
        this.comboBox.setModel(this.myModel)
        this.comboBox.renderer = AglioListCellRenderer()
        this.comboBox.setMinimumAndPreferredWidth(0)
        this.myModel.addElement(toolchainSettings.toolchain())
        this.myModel.selectedItem = toolchainSettings.toolchain()
        this.updateDropDownList()
        this.comboBox.addItemListener { e: ItemEvent ->
            if (e.stateChange == 1) {
                this.handleSelectedItemChange()
            }
        }
    }

    private fun updateDropDownList() {
        val toolchains: LinkedHashSet<AglioToolchain> = LinkedHashSet(knownToolchainInfos)
        toolchains.add(toolchainSettings.toolchain())
        SwingHelper.updateItems(this.comboBox, toolchains.toList(), null)
        // this.comboBox.addItem(DownloadToolchain)

        val selected = toolchainSettings.toolchain()

        if (!Objects.equals(this.comboBox.selectedItem, selected)) {
            this.comboBox.selectedItem = selected
            this.handleSelectedItemChange()
        }
    }

    private fun handleSelectedItemChange() {
        when (val selected = this.getToolchainRef()) {
            is DownloadToolchain -> {
                this.comboBox.setSelectedItem(this.myLastSelectedItem)
                ApplicationManager.getApplication().invokeLater(downloadAction , ModalityState.current())
            }
            is NullToolchain -> Unit
            else -> {
                if (this.myLastSelectedItem != selected && selected.isValid()) {
                    this.myLastSelectedItem = selected
                    this@ToolchainChooserComponent.onSelectAction(selected)
                }
            }
        }
    }

    private fun getToolchainRef(): AglioToolchain {
        var ref = this.comboBox.selectedItem as? AglioToolchain
        if (ref == null) {
            ref = NullToolchain
        }
        return ref
    }

    fun selectedToolchain(): AglioToolchain? {
        return comboBox.selectedItem as? AglioToolchain
    }

    fun refresh() {
        comboBox.removeAllItems()
        knownToolchainInfos = knownToolchains

        updateDropDownList()
    }

    fun select(location: String) {
        if (location.isEmpty()) {
            comboBox.selectedItem = NullToolchain
            return
        }

        val infoToSelect = knownToolchainInfos.find { it.homePath() == location } ?: return
        comboBox.selectedItem = infoToSelect
    }

    private inner class ToolchainComboBoxModel: DefaultComboBoxModel<AglioToolchain>()
}

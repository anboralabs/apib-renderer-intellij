package co.anbora.labs.apiblueprint.viewer.ide.ui.renderer

import co.anbora.labs.apiblueprint.viewer.ide.toolchain.AglioToolchain
import co.anbora.labs.apiblueprint.viewer.ide.toolchain.DownloadToolchain
import co.anbora.labs.apiblueprint.viewer.ide.toolchain.NullToolchain
import co.anbora.labs.apiblueprint.viewer.ide.ui.icons.AglioIcons
import com.intellij.openapi.ui.getPresentablePath
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import javax.swing.JList

class AglioListCellRenderer: ColoredListCellRenderer<AglioToolchain>() {
    override fun customizeCellRenderer(
        list: JList<out AglioToolchain>,
        value: AglioToolchain?,
        index: Int,
        selected: Boolean,
        hasFocus: Boolean,
    ) {
        when {
            value == null -> {
                append(NullToolchain.name())
                return
            }
            value is DownloadToolchain || value is NullToolchain -> {
                append(value.name())
                return
            }
            !value.isValid() -> {
                icon = AglioIcons.AGLIO
                append(value.version())
                append("  ")
                append(getPresentablePath(value.homePath()), SimpleTextAttributes.ERROR_ATTRIBUTES)
            }
            else -> {
                icon = AglioIcons.AGLIO
                append(value.version())
                append("  ")
                append(getPresentablePath(value.homePath()), SimpleTextAttributes.GRAYED_ATTRIBUTES)
            }
        }
    }
}
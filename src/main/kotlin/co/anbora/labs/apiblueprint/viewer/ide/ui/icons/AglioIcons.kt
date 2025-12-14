package co.anbora.labs.apiblueprint.viewer.ide.ui.icons

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object AglioIcons {
    val AGLIO = getIcon("aglio.svg")

    private fun getIcon(path: String): Icon {
        return IconLoader.findIcon("/icons/$path", AglioIcons::class.java.classLoader) as Icon
    }
}
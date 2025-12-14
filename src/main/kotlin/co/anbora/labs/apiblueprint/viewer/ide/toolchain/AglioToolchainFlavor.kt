package co.anbora.labs.apiblueprint.viewer.ide.toolchain

import co.anbora.labs.apiblueprint.viewer.ide.settings.ConfigurationUtil
import com.intellij.openapi.extensions.ExtensionPointName
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isExecutable

abstract class AglioToolchainFlavor {

    fun suggestHomePaths(): Sequence<Path> = getHomePathCandidates().filter { isValidToolchainPath(it) }

    protected abstract fun getHomePathCandidates(): Sequence<Path>

    /**
     * Flavor is added to result in [getApplicableFlavors] if this method returns true.
     * @return whether this flavor is applicable.
     */
    protected open fun isApplicable(): Boolean = true

    /**
     * Checks if the path is the name of a V toolchain of this flavor.
     *
     * @param path path to check.
     * @return true if paths points to a valid home.
     */
    protected open fun isValidToolchainPath(path: Path): Boolean {
        return path.isDirectory()
                && path.resolve(ConfigurationUtil.STANDARD_AGLIO).isExecutable()
    }

    companion object {
        private val EP_NAME: ExtensionPointName<AglioToolchainFlavor> =
            ExtensionPointName.create("co.anbora.labs.apiBlueprint.viewer.toolchain")

        fun getApplicableFlavors(): List<AglioToolchainFlavor> =
            EP_NAME.extensionList.filter { it.isApplicable() }

        fun getFlavor(path: Path): AglioToolchainFlavor? =
            getApplicableFlavors().find { flavor -> flavor.isValidToolchainPath(path) }
    }
}
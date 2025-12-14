package co.anbora.labs.apiblueprint.viewer.ide.discovery

import com.intellij.openapi.extensions.ExtensionPointName
import java.nio.file.Path

abstract class AglioDiscoveryFlavor {

    abstract fun getPathCandidate(): Path?

    /**
     * Flavor is added to result in [getApplicableFlavors] if this method returns true.
     * @return whether this flavor is applicable.
     */
    protected open fun isApplicable(): Boolean = true

    companion object {
        private val EP_NAME: ExtensionPointName<AglioDiscoveryFlavor> =
            ExtensionPointName.create("co.anbora.labs.apiBlueprint.viewer.discovery")

        fun getApplicableFlavors(): List<AglioDiscoveryFlavor> =
            EP_NAME.extensionList.filter { it.isApplicable() }
    }
}

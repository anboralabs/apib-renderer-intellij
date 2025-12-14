package co.anbora.labs.apiblueprint.viewer.ide.discovery.flavor

import co.anbora.labs.apiblueprint.viewer.ide.discovery.AglioDiscoveryFlavor
import co.anbora.labs.apiblueprint.viewer.ide.discovery.Discovery
import com.intellij.openapi.util.SystemInfo
import java.nio.file.Path

class AglioUnixDiscovery: AglioDiscoveryFlavor() {
    override fun getPathCandidate(): Path? {
        return Discovery.unixPath
    }

    override fun isApplicable(): Boolean = SystemInfo.isUnix
}

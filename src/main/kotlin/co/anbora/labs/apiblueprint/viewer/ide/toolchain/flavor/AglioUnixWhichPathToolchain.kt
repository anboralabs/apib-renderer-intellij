package co.anbora.labs.apiblueprint.viewer.ide.toolchain.flavor

import co.anbora.labs.apiblueprint.viewer.ide.discovery.Discovery
import co.anbora.labs.apiblueprint.viewer.ide.toolchain.AglioToolchainFlavor
import com.intellij.openapi.util.SystemInfo
import java.nio.file.Path

class AglioUnixWhichPathToolchain: AglioToolchainFlavor() {

    override fun getHomePathCandidates(): Sequence<Path> {
        val path = Discovery.unixPath
        if (path != null) {
            return listOf(path.parent).asSequence()
        }
        return emptySequence()
    }

    override fun isApplicable(): Boolean = SystemInfo.isUnix
}

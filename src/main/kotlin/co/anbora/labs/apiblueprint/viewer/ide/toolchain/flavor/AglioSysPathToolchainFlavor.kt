package co.anbora.labs.apiblueprint.viewer.ide.toolchain.flavor

import co.anbora.labs.apiblueprint.viewer.ide.toolchain.AglioToolchainFlavor
import co.anbora.labs.apiblueprint.viewer.ide.utils.toPathOrNull
import java.io.File
import java.nio.file.Path
import kotlin.io.path.isDirectory

class AglioSysPathToolchainFlavor : AglioToolchainFlavor() {
    override fun getHomePathCandidates(): Sequence<Path> {
        return System.getenv("PATH")
            .orEmpty()
            .split(File.pathSeparator)
            .asSequence()
            .filter { it.isNotEmpty() }
            .mapNotNull { it.toPathOrNull() }
            .filter { it.isDirectory() }
    }
}

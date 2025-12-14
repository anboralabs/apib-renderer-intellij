package co.anbora.labs.apiblueprint.viewer.ide.toolchain

import co.anbora.labs.apiblueprint.viewer.ide.settings.ConfigurationUtil
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.pathString

interface AglioToolchain {

    fun name(): String
    fun version(): String
    fun stdBinDir(): Path?
    fun rootDir(): Path?
    fun homePath(): String
    fun isValid(): Boolean

    companion object {
        fun fromPath(homePath: String): AglioToolchain {
            if (homePath == "") {
                return NullToolchain
            }

            val path = Path.of(homePath)
            if (!Files.isDirectory(path)) {
                return NullToolchain
            }
            return fromDirectory(path)
        }

        private fun fromDirectory(rootDir: Path): AglioToolchain {
            val version = ConfigurationUtil.guessToolchainVersion(rootDir.pathString)
            return AglioLocalToolchain(version, rootDir)
        }
    }
}
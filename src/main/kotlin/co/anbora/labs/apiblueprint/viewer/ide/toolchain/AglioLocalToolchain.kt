package co.anbora.labs.apiblueprint.viewer.ide.toolchain

import co.anbora.labs.apiblueprint.viewer.ide.settings.ConfigurationUtil
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class AglioLocalToolchain(
    private val version: String,
    private val rootDir: Path,
): AglioToolchain {

    private val homePath = rootDir

    private val executable = rootDir.resolve(ConfigurationUtil.STANDARD_AGLIO)

    override fun name(): String = version

    override fun version(): String = version

    override fun stdBinDir(): Path = executable

    override fun rootDir(): Path = rootDir

    override fun homePath(): String = rootDir().absolutePathString()

    override fun isValid(): Boolean {
        return Files.isExecutable(executable)
    }
}
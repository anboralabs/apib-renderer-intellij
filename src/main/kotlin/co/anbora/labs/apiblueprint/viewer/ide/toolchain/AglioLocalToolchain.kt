package co.anbora.labs.apiblueprint.viewer.ide.toolchain

import java.nio.file.Path

class AglioLocalToolchain(
    private val version: String,
    private val rootDir: Path,
): AglioToolchain {
    override fun name(): String {
        TODO("Not yet implemented")
    }

    override fun version(): String {
        TODO("Not yet implemented")
    }

    override fun stdBinDir(): Path? {
        TODO("Not yet implemented")
    }

    override fun rootDir(): Path? {
        TODO("Not yet implemented")
    }

    override fun homePath(): String {
        TODO("Not yet implemented")
    }

    override fun isValid(): Boolean {
        TODO("Not yet implemented")
    }
}
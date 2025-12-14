package co.anbora.labs.apiblueprint.viewer.ide.toolchain

import java.nio.file.Path

object DownloadToolchain: AglioToolchain {
    override fun name(): String = "Download..."

    override fun version(): String = ""

    override fun stdBinDir(): Path? = null

    override fun rootDir(): Path? = null

    override fun homePath(): String = ""

    override fun isValid(): Boolean = false
}
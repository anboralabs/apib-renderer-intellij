package co.anbora.labs.apiblueprint.viewer.ide.toolchain

object NullToolchain: AglioToolchain {
    override fun name() = ""

    override fun version() = ""

    override fun stdBinDir() = null

    override fun rootDir() = null

    override fun homePath() = ""

    override fun isValid() = false
}
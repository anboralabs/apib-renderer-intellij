package co.anbora.labs.apiblueprint.viewer.ide.runner

import co.anbora.labs.apiblueprint.viewer.ide.settings.ConfigurationUtil
import co.anbora.labs.apiblueprint.viewer.ide.toolchain.AglioToolchainService
import java.io.File

object AglioRunner {

    /**
     * Renders an API Blueprint file to HTML using Aglio.
     *
     * @param inputFilePath The path to the .apib file to render
     * @return The rendered HTML string, or null if rendering fails
     */
    fun renderToHtml(inputFilePath: String): String? {
        val toolchain = AglioToolchainService.toolchainSettings.toolchain()

        if (!toolchain.isValid()) {
            return null
        }

        val aglioPath = toolchain.stdBinDir()?.toAbsolutePath()?.toString() ?: return null

        return try {
            // Crear archivo temporal para el output
            val tempFile = File.createTempFile("aglio-output-", ".html")
            tempFile.deleteOnExit()

            // Ejecutar: aglio -i input.apib -o output.html
            ConfigurationUtil.executeAndReturnOutput(
                aglioPath,
                "-i", inputFilePath,
                "-o", tempFile.absolutePath
            )

            // Leer el HTML generado
            tempFile.readText()
        } catch (e: Exception) {
            null
        }
    }
}
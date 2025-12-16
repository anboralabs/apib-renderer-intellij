package co.anbora.labs.apiblueprint.viewer.ide.ui.preview

import co.anbora.labs.apiblueprint.viewer.ide.settings.ConfigurationUtil
import co.anbora.labs.apiblueprint.viewer.ide.toolchain.AglioToolchainService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.jcef.JBCefBrowser
import org.intellij.plugins.markdown.ui.preview.MarkdownHtmlPanel
import org.intellij.plugins.markdown.ui.preview.MarkdownHtmlPanelProvider
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JComponent

class ApiBluePrintPreviewProvider : MarkdownHtmlPanelProvider() {
    private val log = Logger.getInstance(ApiBluePrintPreviewProvider::class.java)

    override fun createHtmlPanel(): MarkdownHtmlPanel {
        return ApiBluePrintHtmlPanel(log)
    }

    override fun isAvailable(): AvailabilityInfo = AvailabilityInfo.AVAILABLE

    override fun getProviderInfo(): ProviderInfo = ProviderInfo(
        "API Blueprint Cached Preview",
        "org.intellij.plugins.markdown.ui.preview.jcef.JCEFHtmlPanelProvider"
    )
}

/**
 * Panel que renderiza archivos .apib usando Aglio con JBCefBrowser.
 */
private class ApiBluePrintHtmlPanel(
    private val log: Logger
) : MarkdownHtmlPanel {

    // Browser JCEF para renderizar HTML
    private val browser = JBCefBrowser()

    // Estado del renderizado actual
    private val isRendering = AtomicBoolean(false)
    @Volatile private var currentFile: VirtualFile? = null
    @Volatile private var lastHtml: String = ""

    override fun getComponent(): JComponent = browser.component

    override fun setHtml(html: String, initialScrollOffset: Int, document: VirtualFile?) {
        currentFile = document

        // Si es un archivo .apib, usar Aglio para renderizar
        if (document != null && document.extension == "apib") {
            renderApiBlueprintFile(document, initialScrollOffset)
            return
        }

        // Para otros archivos, cargar HTML directamente
        lastHtml = html
        browser.loadHTML(html)
    }

    private fun renderApiBlueprintFile(file: VirtualFile, scrollOffset: Int) {
        val toolchain = AglioToolchainService.toolchainSettings.toolchain()

        if (!toolchain.isValid()) {
            val errorHtml = createErrorHtml(
                "Aglio not configured",
                "Please configure Aglio toolchain in Settings > Tools > API Blueprint"
            )
            lastHtml = errorHtml
            browser.loadHTML(errorHtml)
            return
        }

        // Mostrar loading mientras se renderiza
        if (!isRendering.getAndSet(true)) {
            val loadingHtml = createLoadingHtml()
            lastHtml = loadingHtml
            browser.loadHTML(loadingHtml)

            // Renderizar con Aglio en background
            ApplicationManager.getApplication().executeOnPooledThread {
                val html = try {
                    renderWithAglio(
                        toolchain.stdBinDir()?.toAbsolutePath().toString(),
                        file.path
                    )
                } catch (e: Exception) {
                    log.warn("Failed to render with Aglio", e)
                    createErrorHtml("Render failed", escapeHtml(e.message ?: e.javaClass.name))
                }

                // Actualizar el browser con el HTML generado
                isRendering.set(false)
                lastHtml = html
                browser.loadHTML(html)
            }
        }
    }

    private fun renderWithAglio(aglioPath: String, inputFilePath: String): String {
        try {
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
            return tempFile.readText()
        } catch (e: Exception) {
            throw RuntimeException("Failed to execute Aglio: ${e.message}", e)
        }
    }

    private fun createLoadingHtml(): String = """
        <html>
          <head>
            <style>
              body {
                display: flex;
                justify-content: center;
                align-items: center;
                height: 100vh;
                margin: 0;
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                background: #f5f5f5;
              }
              .loading-container {
                text-align: center;
              }
              .spinner {
                border: 4px solid #f3f3f3;
                border-top: 4px solid #3498db;
                border-radius: 50%;
                width: 40px;
                height: 40px;
                animation: spin 1s linear infinite;
                margin: 0 auto 20px;
              }
              @keyframes spin {
                0% { transform: rotate(0deg); }
                100% { transform: rotate(360deg); }
              }
              .loading-text {
                color: #555;
                font-size: 16px;
              }
            </style>
          </head>
          <body>
            <div class="loading-container">
              <div class="spinner"></div>
              <div class="loading-text">Rendering API Blueprint with Aglio...</div>
            </div>
          </body>
        </html>
    """.trimIndent()

    private fun createErrorHtml(title: String, message: String): String = """
        <html>
          <head>
            <style>
              body {
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                padding: 20px;
                background: #fff;
              }
              .error-container {
                max-width: 600px;
                margin: 40px auto;
                padding: 20px;
                background: #fff3cd;
                border: 1px solid #ffc107;
                border-radius: 4px;
              }
              .error-title {
                color: #856404;
                font-size: 20px;
                font-weight: 600;
                margin: 0 0 10px 0;
              }
              .error-message {
                color: #856404;
                font-size: 14px;
                margin: 0;
                white-space: pre-wrap;
              }
            </style>
          </head>
          <body>
            <div class="error-container">
              <h3 class="error-title">$title</h3>
              <p class="error-message">$message</p>
            </div>
          </body>
        </html>
    """.trimIndent()

    override fun reloadWithOffset(offset: Int) {
        // Recargar el HTML actual
        browser.loadHTML(lastHtml)
    }

    override fun addScrollListener(listener: MarkdownHtmlPanel.ScrollListener) {
        // JBCefBrowser no soporta scroll listeners directamente
        // Este método se mantiene vacío por compatibilidad con la interfaz
    }

    override fun removeScrollListener(listener: MarkdownHtmlPanel.ScrollListener) {
        // JBCefBrowser no soporta scroll listeners directamente
        // Este método se mantiene vacío por compatibilidad con la interfaz
    }

    override fun dispose() {
        browser.dispose()
    }

    private fun escapeHtml(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
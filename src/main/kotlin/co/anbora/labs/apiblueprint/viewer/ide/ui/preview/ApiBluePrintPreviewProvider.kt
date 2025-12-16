package co.anbora.labs.apiblueprint.viewer.ide.ui.preview

import co.anbora.labs.apiblueprint.viewer.ide.settings.ConfigurationUtil
import co.anbora.labs.apiblueprint.viewer.ide.toolchain.AglioToolchainService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.concurrency.AppExecutorUtil
import org.intellij.plugins.markdown.ui.preview.MarkdownHtmlPanel
import org.intellij.plugins.markdown.ui.preview.MarkdownHtmlPanelProvider
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import javax.swing.JComponent
import javax.swing.JEditorPane
import javax.swing.JScrollPane
import javax.swing.event.HyperlinkEvent

class ApiBluePrintPreviewProvider : MarkdownHtmlPanelProvider() {
    private val log = Logger.getInstance(ApiBluePrintPreviewProvider::class.java)

    override fun createHtmlPanel(): MarkdownHtmlPanel = CachedMarkdownHtmlPanel(log)

    override fun isAvailable(): AvailabilityInfo = AvailabilityInfo.AVAILABLE

    override fun getProviderInfo(): ProviderInfo = ProviderInfo("API Blueprint Cached Preview", "org.intellij.plugins.markdown.ui.preview.jcef.JCEFHtmlPanelProvider")
}

internal data class CacheKey(
    val fileId: String?,
    val settingsSignature: String,
)

internal data class CacheEntry(
    val lastHash: String,
    val lastHtml: String,
)

/**
 * Motor de caché reutilizable y testeable sin dependencias de UI.
 */
internal class RenderCache {
    private val map = ConcurrentHashMap<CacheKey, CacheEntry>()

    fun get(key: CacheKey): CacheEntry? = map[key]

    fun put(key: CacheKey, entry: CacheEntry) {
        map[key] = entry
    }

    fun clearForSettingsSignature(signature: String) {
        map.keys.removeIf { it.settingsSignature == signature }
    }
}

private class CachedMarkdownHtmlPanel(private val log: Logger) : MarkdownHtmlPanel {
    private val editor = JEditorPane("text/html", "")
    private val component: JComponent = JScrollPane(editor)

    private val cache = RenderCache()
    @Volatile private var lastKey: CacheKey? = null
    @Volatile private var currentCss: String? = null

    private val scheduler = AppExecutorUtil.getAppScheduledExecutorService()
    private val pendingTask = AtomicReference<ScheduledFuture<*>?>()

    // Estado simple del panel
    @Volatile private var lastRenderedHtml: String = ""

    init {
        editor.isEditable = false
        editor.addHyperlinkListener { e ->
            if (e.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                // Dejar que la plataforma gestione o abrir en navegador predeterminado
                // No crítico para la prueba de caché
            }
        }
    }

    override fun getComponent(): JComponent = component

    override fun setHtml(html: String, initialScrollOffset: Int, document: VirtualFile?) {
        // Si es un archivo .apib, usar Aglio para renderizar
        if (document != null && document.extension == "apib") {
            renderApiBlueprintFile(document)
            return
        }

        // Para otros archivos, usar el HTML proporcionado
        lastRenderedHtml = html
        ApplicationManager.getApplication().invokeLater({
            editor.text = html
            // scroll básico por referencia si el offset es 0 se ignora; no mapeamos offset->anchor aquí
        }, ModalityState.any())
    }

    private fun renderApiBlueprintFile(file: VirtualFile) {
        val toolchain = AglioToolchainService.toolchainSettings.toolchain()

        if (!toolchain.isValid()) {
            val errorHtml = """
                <html>
                  <body>
                    <div style='color: red; font-family: monospace; padding: 20px;'>
                      <h3>Aglio not configured</h3>
                      <p>Please configure Aglio toolchain in Settings > Tools > API Blueprint</p>
                    </div>
                  </body>
                </html>
            """.trimIndent()
            lastRenderedHtml = errorHtml
            ApplicationManager.getApplication().invokeLater({
                editor.text = errorHtml
            }, ModalityState.any())
            return
        }

        // Renderizar con Aglio en background
        ApplicationManager.getApplication().executeOnPooledThread {
            val html = try {
                renderWithAglio(toolchain.stdBinDir()?.toAbsolutePath().toString(), file.path)
            } catch (e: Exception) {
                log.warn("Failed to render with Aglio", e)
                safeErrorHtml(e)
            }

            lastRenderedHtml = html
            ApplicationManager.getApplication().invokeLater({
                editor.text = html
            }, ModalityState.any())
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

    override fun reloadWithOffset(offset: Int) {
        // Re-carga el último HTML conocido manteniendo offset aproximado
        setHtml(lastRenderedHtml, offset, null as VirtualFile?)
    }

    private val listeners = mutableSetOf<MarkdownHtmlPanel.ScrollListener>()

    override fun addScrollListener(listener: MarkdownHtmlPanel.ScrollListener) {
        listeners.add(listener)
    }

    override fun removeScrollListener(listener: MarkdownHtmlPanel.ScrollListener) {
        listeners.remove(listener)
    }

    override fun dispose() {
        pendingTask.getAndSet(null)?.cancel(false)
        listeners.clear()
    }

    // API auxiliar específica para este panel
    fun renderContent(fileId: String?, content: String, settingsSignature: String, debounceMs: Long = 75) {
        val key = CacheKey(fileId, settingsSignature)
        val hash = sha256("$settingsSignature:::$content")

        // Hit directo
        cache.get(key)?.takeIf { it.lastHash == hash }?.let { hit ->
            log.debug("[MarkdownPreview] cache HIT for $key")
            setHtml(hit.lastHtml, 0, null as VirtualFile?)
            lastKey = key
            return
        }

        // Debounce para ráfagas
        pendingTask.getAndSet(null)?.cancel(false)
        val task = scheduler.schedule({
            val start = System.nanoTime()
            val html = try {
                markdownToHtml(content, settingsSignature, currentCss)
            } catch (t: Throwable) {
                log.warn("Markdown render failed", t)
                safeErrorHtml(t)
            }

            val tookMs = (System.nanoTime() - start) / 1_000_000
            log.debug("[MarkdownPreview] render MISS for $key in ${tookMs}ms")

            cache.put(key, CacheEntry(hash, html))
            lastKey = key
            setHtml(html, 0, null as VirtualFile?)
        }, debounceMs, TimeUnit.MILLISECONDS)
        pendingTask.set(task)
    }
}

// ---- Helpers reutilizables/testeables ----
internal fun sha256(text: String): String {
    val md = MessageDigest.getInstance("SHA-256")
    return md.digest(text.toByteArray(Charsets.UTF_8)).joinToString(separator = "") { b -> "%02x".format(b) }
}

internal fun markdownToHtml(content: String, settingsSignature: String, css: String?): String {
    // En una implementación real, invocaríamos el renderer del plugin de Markdown y aplicaríamos CSS/settings
    val cssBlock = css?.let { "<style>\n$it\n</style>" } ?: ""
    return """
        <html>
          <head>
            $cssBlock
          </head>
          <body>
            <pre>${escapeHtml(content)}</pre>
          </body>
        </html>
    """.trimIndent()
}

internal fun escapeHtml(s: String): String =
    s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

internal fun safeErrorHtml(t: Throwable): String =
    """
        <html>
          <body>
            <div style='color: red; font-family: monospace;'>Render error: ${escapeHtml(t.message ?: t.javaClass.name)}</div>
          </body>
        </html>
    """.trimIndent()
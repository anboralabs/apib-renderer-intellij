package co.anbora.labs.apiblueprint.viewer.ide.editor

import co.anbora.labs.apiblueprint.viewer.ide.index.ApibHtmlCache
import co.anbora.labs.apiblueprint.viewer.ide.utils.isApiBFile
import com.intellij.openapi.fileEditor.AsyncFileEditorProvider
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.jcef.JBCefBrowser
import java.beans.PropertyChangeListener
import javax.swing.JComponent

private const val EDITOR_TYPE_ID = "79f770ae-4b71-4fd8-989c-be5cf68d9baf.co.anbora.labs.apiblueprint.viewer.ide.editor"

class AglioEditorProvider: FileEditorProvider, DumbAware {
    override fun accept(
        project: Project,
        vFile: VirtualFile
    ): Boolean = vFile.isApiBFile()

    override fun createEditor(
        project: Project,
        vFile: VirtualFile
    ): FileEditor {
        return AglioFileEditor(vFile)
    }

    override fun getEditorTypeId(): String = EDITOR_TYPE_ID

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR
}

private class AglioFileEditor(
    private val virtualFile: VirtualFile
) : UserDataHolderBase(), FileEditor {

    private val browser = JBCefBrowser()

    init {
        loadHtml()
    }

    private fun loadHtml() {
        // Obtener el documento actual del archivo
        val document = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance()
            .getDocument(virtualFile)

        if (document != null) {
            // Intentar obtener el HTML desde la caché
            val htmlContent = ApibHtmlCache.getInstance().getHtml(virtualFile.path, document.text)

            if (htmlContent != null) {
                browser.loadHTML(htmlContent)
            } else {
                // Si no hay HTML en caché, mostrar mensaje de espera
                val waitingHtml = createWaitingHtml()
                browser.loadHTML(waitingHtml)
            }
        }
    }

    private fun createWaitingHtml(): String = """
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
              .message-container {
                text-align: center;
                padding: 40px;
                background: white;
                border-radius: 8px;
                box-shadow: 0 2px 8px rgba(0,0,0,0.1);
              }
              .message-text {
                color: #555;
                font-size: 16px;
              }
            </style>
          </head>
          <body>
            <div class="message-container">
              <div class="message-text">Waiting for HTML to be rendered...</div>
              <div style="margin-top: 10px; color: #888; font-size: 14px;">
                The preview will appear once the file is processed.
              </div>
            </div>
          </body>
        </html>
    """.trimIndent()

    override fun getComponent(): JComponent = browser.component

    override fun getPreferredFocusedComponent(): JComponent? = browser.component

    override fun getName(): String = "API Blueprint Preview"

    override fun setState(state: FileEditorState) {}

    override fun isModified(): Boolean = false

    override fun isValid(): Boolean = true

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {}

    override fun removePropertyChangeListener(listener: PropertyChangeListener) {}

    override fun getCurrentLocation(): FileEditorLocation? = null

    override fun dispose() {
        browser.dispose()
    }

    override fun getFile(): VirtualFile = virtualFile
}
package co.anbora.labs.apiblueprint.viewer.ide.editor

import co.anbora.labs.apiblueprint.viewer.ide.index.ApibHtmlCache
import co.anbora.labs.apiblueprint.viewer.ide.index.ApibHtmlCacheListener
import co.anbora.labs.apiblueprint.viewer.ide.toolchain.AglioToolchainService.Companion.toolchainSettings
import co.anbora.labs.apiblueprint.viewer.ide.utils.isApiBFile
import com.intellij.openapi.application.ApplicationManager
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
import com.intellij.util.messages.MessageBusConnection
import java.beans.PropertyChangeListener
import javax.swing.JComponent

private const val EDITOR_TYPE_ID = "79f770ae-4b71-4fd8-989c-be5cf68d9baf.co.anbora.labs.apiblueprint.viewer.ide.editor"

class AglioEditorProvider: FileEditorProvider, DumbAware {
    override fun accept(
        project: Project,
        vFile: VirtualFile
    ): Boolean = vFile.isApiBFile() && toolchainSettings.toolchain().isValid()

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
    private val messageBusConnection: MessageBusConnection = ApplicationManager.getApplication().messageBus.connect()

    init {
        loadHtml()
        setupCacheListener()
    }

    /**
     * Sets up listener for HTML cache updates to automatically re-render the preview.
     */
    private fun setupCacheListener() {
        messageBusConnection.subscribe(ApibHtmlCacheListener.TOPIC, ApibHtmlCacheListener { filePath, html ->
            // Only re-render if the cached HTML is for this editor's file
            if (filePath == virtualFile.path) {
                browser.loadHTML(html)
            }
        })
    }

    private fun loadHtml() {
        val document = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance()
            .getDocument(virtualFile)

        if (document != null) {
            val htmlContent = ApibHtmlCache.getInstance().getHtml(virtualFile.path, document.text)

            if (htmlContent != null) {
                browser.loadHTML(htmlContent)
            } else {
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
        messageBusConnection.disconnect()
        browser.dispose()
    }

    override fun getFile(): VirtualFile = virtualFile
}
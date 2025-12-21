package co.anbora.labs.apiblueprint.viewer.ide.annotators

import co.anbora.labs.apiblueprint.viewer.ide.index.ApibHtmlCache
import co.anbora.labs.apiblueprint.viewer.ide.inspections.PreviewRenderInspection
import co.anbora.labs.apiblueprint.viewer.ide.runner.AglioRunner
import co.anbora.labs.apiblueprint.viewer.ide.utils.isApiBFile
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.PsiFile

private val NO_PROBLEMS_FOUND: RenderExternalAnnotator.Results = RenderExternalAnnotator.Results(emptyList())

class RenderExternalAnnotator: ExternalAnnotator<RenderExternalAnnotator.State, RenderExternalAnnotator.Results>() {

    data class State(
        val file: PsiFile,
        val document: Document
    )

    data class Results(val issues: List<Problem>)

    private val log = Logger.getInstance(
        RenderExternalAnnotator::class.java
    )

    override fun collectInformation(file: PsiFile): State? {
        val vfile = file.virtualFile

        if (vfile == null) {
            log.info("Missing vfile for $file")
            return null
        }
        // collect the document here because doAnnotate has no read access to the file document manager
        val document = FileDocumentManager.getInstance().getDocument(vfile)

        if (document == null) {
            log.info("Missing document")
            return null
        }

        if (!vfile.isApiBFile()) {
            return null
        }

        return State(file, document)
    }

    override fun doAnnotate(collectedInfo: State?): Results {
        if (collectedInfo == null) {
            return NO_PROBLEMS_FOUND
        }

        val (file, document) = collectedInfo

        val htmlContent = ApibHtmlCache.getInstance().getHtml(file.virtualFile.path, document.text)

        if (htmlContent == null) {
            // Ejecutar aglio para generar el HTML
            val renderedHtml = AglioRunner.renderToHtml(file.virtualFile.path)

            // Si se generó correctamente, guardar en la caché
            if (renderedHtml != null) {
                ApibHtmlCache.getInstance().putHtml(file.virtualFile.path, document.text, renderedHtml)
                log.info("HTML generado y guardado en caché para ${file.virtualFile.path}")
            } else {
                log.warn("No se pudo generar HTML con Aglio para ${file.virtualFile.path}")
            }
        }

        return NO_PROBLEMS_FOUND
    }

    override fun getPairedBatchInspectionShortName(): String = PreviewRenderInspection::class.java.name
}
package co.anbora.labs.apiblueprint.viewer.ide.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ex.ExternalAnnotatorBatchInspection

class PreviewRenderInspection: LocalInspectionTool(), ExternalAnnotatorBatchInspection {

    override fun getShortName(): String  = PreviewRenderInspection::class.java.name
}
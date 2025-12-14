package co.anbora.labs.apiblueprint.viewer.ide.startup

import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.javascript.nodejs.npm.NpmManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class CheckApiBluePrintRenderer: ProjectActivity {
    override suspend fun execute(project: Project) {
        val npmManager = NpmManager.getInstance(project)
        val interpreter = NodeJsInterpreterManager.getInstance(project).interpreter

    }
}
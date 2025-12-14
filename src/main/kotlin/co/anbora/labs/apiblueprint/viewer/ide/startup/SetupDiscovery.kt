package co.anbora.labs.apiblueprint.viewer.ide.startup

import co.anbora.labs.apiblueprint.viewer.ide.discovery.AglioDiscoveryFlavor
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import java.util.logging.Logger

class SetupDiscovery: ProjectActivity {

    private val logger = Logger.getLogger(SetupDiscovery::class.simpleName)

    override suspend fun execute(project: Project) {
        AglioDiscoveryFlavor.getApplicableFlavors().forEach {
            logger.info {
                "toolchain path: ${it.getPathCandidate()}"
            }
        }
    }
}
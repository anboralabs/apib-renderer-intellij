package co.anbora.labs.apiblueprint.viewer.ide.startup

import co.anbora.labs.apiblueprint.ide.notifications.ApiBlueprintNotifications
import co.anbora.labs.apiblueprint.viewer.ide.actions.Setup
import co.anbora.labs.apiblueprint.viewer.ide.discovery.AglioDiscoveryFlavor
import co.anbora.labs.apiblueprint.viewer.ide.toolchain.AglioToolchainService.Companion.toolchainSettings
import com.intellij.notification.NotificationType
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

        val toolchain = toolchainSettings.toolchain()
        if (!toolchain.isValid()) {
            val notification = ApiBlueprintNotifications.createNotification(
                "API Blueprint Viewer",
                "Setup aglio renderer tool",
                NotificationType.WARNING,
                Setup()
            )
            ApiBlueprintNotifications.showNotification(notification, project)
        }
    }
}
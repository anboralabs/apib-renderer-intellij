package co.anbora.labs.apiblueprint.viewer.ide.actions

import co.anbora.labs.apiblueprint.viewer.ide.settings.AglioSettingsConfigurable
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.ProjectManager

class Setup: NotificationAction("Setup") {

    override fun actionPerformed(
        e: AnActionEvent,
        notification: Notification
    ) {
        val project = e.project ?: ProjectManager.getInstance().defaultProject
        AglioSettingsConfigurable.show(project)
        notification.expire()
    }
}

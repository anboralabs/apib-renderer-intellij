package co.anbora.labs.apiblueprint.viewer.ide.notifications

import co.anbora.labs.apiblueprint.viewer.ide.actions.BuyLicense
import co.anbora.labs.apiblueprint.viewer.ide.ui.icons.AglioIcons
import com.intellij.notification.Notification
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import javax.swing.Icon

object AglioRendererNotifications {
    @JvmStatic
    fun createNotification(
        title: String,
        content: String,
        type: NotificationType,
        notificationGroup: String,
        icon: Icon,
        vararg actions: AnAction
    ): Notification {
        val notification = NotificationGroupManager.getInstance()
            .getNotificationGroup(notificationGroup)
            .createNotification(content, type)
            .setTitle(title)
            .setIcon(icon)

        for (action in actions) {
            notification.addAction(action)
        }

        return notification
    }

    @JvmStatic
    fun showNotification(notification: Notification, project: Project?) {
        try {
            notification.notify(project)
        } catch (e: Exception) {
            notification.notify(project)
        }
    }

    @JvmStatic
    fun createNotification(
        title: String,
        content: String,
        type: NotificationType,
        vararg actions: AnAction
    ): Notification {
        return createNotification(
            title,
            content,
            type,
            "3fc30685-7463-4ec0-91d4-c21567ef00e3_Aglio_Notification",
            AglioIcons.AGLIO_40,
            *actions
        )
    }

    @JvmStatic
    fun supportNotification(project: Project?) {
        val notification = createNotification(
            "Support API Blueprint Viewer Plugin",
            "Buy the license; 15 USD lifetime",
            NotificationType.WARNING,
            BuyLicense()
        )

        showNotification(notification, project)
    }
}
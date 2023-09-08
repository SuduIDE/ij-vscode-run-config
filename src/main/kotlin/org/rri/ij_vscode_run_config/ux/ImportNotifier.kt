package org.rri.ij_vscode_run_config.ux

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import org.rri.ij_vscode_run_config.ux.actions.ImportAllAction
import org.rri.ij_vscode_run_config.ux.actions.ImportSpecificAction

object ImportNotifier {

    fun notifyInfo(project: Project, content: String) {
        val tmp: StringBuilder = StringBuilder()
        for (i in 1..100) {
            tmp.append("$i <br>")
        }

        NotificationGroupManager.getInstance()
            .getNotificationGroup("VSCode Import Notification Group")
            .createNotification(content, NotificationType.INFORMATION)
            .addAction(ImportAllAction("Import All"))
            .addAction(ImportSpecificAction("Choose Specific..."))
            .setSuggestionType(true)
            .setContent("Detected VSCode Configurations which can be imported")
            .setTitle("VSCode Import Plugin")
            .notify(project)
    }

    fun notifyWarning(project: Project, content: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("VSCode Import Notification Group")
            .createNotification("Foo_VSCode configuration <br/><br/> Already have configuration with the same properties: Foo_IJ_IDEA", NotificationType.WARNING)
            .setTitle("VSCode Import Plugin")
            .notify(project)
    }

    fun notifyError(project: Project, content: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("VSCode Import Notification Group")
            .createNotification("Config_With_Error configuration <br/><br/> Invalid MainClass property: /bad/path/to/main/class", NotificationType.ERROR)
            .setTitle("VSCode Import Plugin")
            .notify(project)
    }

}
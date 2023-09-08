package org.rri.ij_vscode_run_config.ux

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity


class PostStartupActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
//        ImportNotifier.notifyInfo(project, "You can import your run configurations from VSCode")
//        ImportNotifier.notifyWarning(project, "You can import your run configurations from VSCode")
//        ImportNotifier.notifyError(project, "You can import your run configurations from VSCode")
    }

}
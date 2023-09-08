package org.rri.ij_vscode_run_config.ux.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.util.NlsActions.ActionDescription
import com.intellij.openapi.util.NlsActions.ActionText
import com.intellij.openapi.vfs.VirtualFileManager
import org.rri.ij_vscode_run_config.logic.ImportManager
import org.rri.ij_vscode_run_config.logic.ImportProblems
import org.rri.ij_vscode_run_config.ux.ImportDialogWrapper
import java.util.function.Supplier
import javax.swing.Icon


class ImportSpecificAction : AnAction {

    constructor() : super()
    constructor(icon: Icon?) : super(icon)
    constructor(@ActionText text: String?) : super(text)
    constructor(dynamicText: Supplier<@ActionText String>) : super(dynamicText)
    constructor(text: @ActionText String?, description: @ActionDescription String?, icon: Icon?) : super(
        text,
        description,
        icon
    )

    constructor(dynamicText: Supplier<@ActionText String>, icon: Icon?) : super(dynamicText, icon)
    constructor(
        dynamicText: Supplier<@ActionText String>,
        dynamicDescription: Supplier<@ActionDescription String>,
        icon: Icon?
    ) : super(dynamicText, dynamicDescription, icon)


    override fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible = event.project != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(event: AnActionEvent) {
        if (event.project == null)
            return

        VirtualFileManager.getInstance().syncRefresh()

        val manager = ImportManager(event.project!!, event.dataContext)
        val problems: Map<String, ImportProblems> = manager.deserialize()

        val dialog = ImportDialogWrapper(event.project!!, manager.getConfigurations(), problems)

        if (dialog.showAndGet()) {
            val configs: Set<String> = dialog.getSelectedConfigurations()
            if (dialog.getIsTreeView()) {
                manager.importConfigurationsWithDependencies(configs)
            } else {
                manager.importConfigurations(configs)
            }
        }
    }

}
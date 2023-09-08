package org.rri.ij_vscode_run_config.ux.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.util.NlsActions.ActionText
import com.intellij.openapi.util.NlsActions.ActionDescription
import com.intellij.openapi.vfs.VirtualFileManager
import org.rri.ij_vscode_run_config.logic.ImportManager
import java.util.function.Supplier
import javax.swing.Icon


class ImportAllAction : AnAction {

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
        super.update(event)
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(event: AnActionEvent) {
        if (event.project == null)
            return

        VirtualFileManager.getInstance().syncRefresh()
        val importManager = ImportManager(event.project!!, event.dataContext)
        importManager.deserialize()
        importManager.importAllConfigurations()
    }

}
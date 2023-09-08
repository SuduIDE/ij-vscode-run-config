package org.rri.ij_vscode_run_config.logic

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import java.nio.file.Path

class VSCodeFolderListener(private val project : Project) : BulkFileListener {

    override fun after(events: MutableList<out VFileEvent>) {
        val service = project.service<VSCodeFolderService>()

        for (vfEvent in events) {
            when (Path.of(vfEvent.path)) {
                service.vsCodeFolderPath -> service.setVSCodeFolder(vfEvent.file)
                service.launchFilePath -> service.setLaunchFile(vfEvent.file)
                service.settingsFilePath -> service.setSettingsFile(vfEvent.file)
                service.tasksFilePath -> service.setTasksFile(vfEvent.file)
            }
        }

        super.after(events)
    }

}
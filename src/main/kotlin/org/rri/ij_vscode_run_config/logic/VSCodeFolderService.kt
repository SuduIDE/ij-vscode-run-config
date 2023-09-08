package org.rri.ij_vscode_run_config.logic

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.readText
import kotlinx.serialization.json.*
import java.nio.file.Path


@Service(Service.Level.PROJECT)
class VSCodeFolderService(private val project: Project) {

    companion object {
        const val VSCODE_FOLDER_NAME: String = ".vscode"
        const val LAUNCH_FILENAME: String = "launch.json"
        const val SETTINGS_FILENAME: String = "settings.json"
        const val TASKS_FILENAME: String = "tasks.json"
    }

    private var vsCodeVFolder: VirtualFile? = null
    val vsCodeFolderPath: Path

    private var launchVFile: VirtualFile? = null
    val launchFilePath: Path

    private var settingsVFile: VirtualFile? = null
    val settingsFilePath: Path

    private var tasksVFile: VirtualFile? = null
    val tasksFilePath: Path

    init {
        val projectDirPath = project.guessProjectDir()!!.toNioPath()

        vsCodeFolderPath = projectDirPath.resolve(VSCODE_FOLDER_NAME)
        launchFilePath = vsCodeFolderPath.resolve(LAUNCH_FILENAME)
        settingsFilePath = vsCodeFolderPath.resolve(SETTINGS_FILENAME)
        tasksFilePath = vsCodeFolderPath.resolve(TASKS_FILENAME)

        vsCodeVFolder = VirtualFileManager.getInstance().findFileByNioPath(vsCodeFolderPath)
        launchVFile = VirtualFileManager.getInstance().findFileByNioPath(launchFilePath)
        settingsVFile = VirtualFileManager.getInstance().findFileByNioPath(settingsFilePath)
        tasksVFile = VirtualFileManager.getInstance().findFileByNioPath(tasksFilePath)
    }

    fun isValid(): Boolean =
        project.isInitialized &&
        project.isOpen &&
        (vsCodeVFolder?.isValid == true) &&
        (launchVFile?.isValid == true)

    fun setVSCodeFolder(folder: VirtualFile?) {
        vsCodeVFolder = folder

        launchVFile = VirtualFileManager.getInstance().refreshAndFindFileByNioPath(launchFilePath)
        settingsVFile = VirtualFileManager.getInstance().refreshAndFindFileByNioPath(settingsFilePath)
        tasksVFile = VirtualFileManager.getInstance().refreshAndFindFileByNioPath(tasksFilePath)
    }

    fun setLaunchFile(file: VirtualFile?) {
        launchVFile = file
    }

    fun setSettingsFile(file: VirtualFile?) {
        settingsVFile = file
    }

    fun setTasksFile(file: VirtualFile?) {
        tasksVFile = file
    }

    fun getLaunchJson(): JsonElement? {
        return getJsonElement(launchVFile)
    }

    fun getSettingsJson(): JsonElement? {
        return getJsonElement(settingsVFile)
    }

    fun getTasksJson(): JsonElement? {
        return getJsonElement(tasksVFile)
    }

    private fun getJsonElement(file : VirtualFile?) : JsonElement? {
        val json: JsonElement? = Json.runCatching { parseToJsonElement(file!!.readText()) }.getOrNull()
        return json?.jsonObject
    }

}
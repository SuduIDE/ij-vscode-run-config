package org.rri.ij_vscode_run_config

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.*
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScopes
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*
import java.nio.charset.StandardCharsets


class ImportAction : AnAction() {

    // ! НИКАКОГО СОСТОЯНИЯ

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible = event.project != null
    }

    override fun actionPerformed(event: AnActionEvent) {
        // * "projectName"
        // * Получаем из самого проекта (в идее каждому приложению соответствует свое окно)
        if (event.project == null)
            return

        val runManager = RunManager.getInstance(event.project!!)
        val vscodeFolder: VirtualFile = event.project!!.guessProjectDir()?.findChild(".vscode") ?: return
        val launchFile: VirtualFile = FilenameIndex.getVirtualFilesByName("launch.json",
                GlobalSearchScopes.directoryScope(
                    event.project!!, vscodeFolder, false)
            ).first() ?: return

        if (!launchFile.isValid)
            return

        val content = String(launchFile.contentsToByteArray(), StandardCharsets.UTF_8)
        val json: JsonElement = try {Json.parseToJsonElement(content)} catch (exc: SerializationException) { return }
        for (currVSConfigElement: JsonElement in json.jsonObject["configurations"]?.jsonArray!!) {
            val currVSConfig: JsonObject = currVSConfigElement.jsonObject

            // * "type"
            if (currVSConfig["type"]?.jsonPrimitive?.content != "java")
                continue

            // & "request"
            if (currVSConfig["request"]?.jsonPrimitive?.content == "launch") {
                val nameStr = currVSConfig["name"]?.jsonPrimitive?.content!!
                if (VariableRepository.getInstance().contains(nameStr))
                    continue

//                val sourcePaths = ???
//                val JDK = ???
//                val beforeRunTasks = appCfg.beforeRunTasks
//                val encoding = currVSConfig["encoding"]
//                val classpathModifications = appCfg.classpathModifications
//                val javaExec = currVSConfig["javaExec"]
//                val stepFilters = currVSConfig["stepFilters"]
//                val inputRedirect = ???
//                val suppressMultipleSessionWarning = ???

                val javaAppCfgBuilder = JavaAppConfigBuilder(nameStr, event, currVSConfig)
                try {
                    val config: RunnerAndConfigurationSettings = javaAppCfgBuilder
                        .setMainClass()
                        .setProgramArgs()
                        .setModulePaths()
                        .setClassPaths()
                        .setVMArgs()
                        .setWorkingDirectory()
                        .setEnv()
                        .setShortenCommandLine()
                        .setEncoding()
                        .build(runManager)
                    runManager.addConfiguration(config)
                    println("YEP $nameStr")
                } catch (exc: ImportException) {
                    println(exc.message)
                } catch (exc: RuntimeConfigurationException) {
                    println(exc.message)
                } catch (exc: NullPointerException) {
                    println(exc.message)
                }
            } else if (currVSConfig["request"]?.jsonPrimitive?.content  == "attach") {

            } else {
                Messages.showMessageDialog("Undefined configuration request type!", "Error", Messages.getErrorIcon());
            }
        }

        println(json)
    }

}

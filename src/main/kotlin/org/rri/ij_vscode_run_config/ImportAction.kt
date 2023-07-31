package org.rri.ij_vscode_run_config

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.*
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScopes
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*


class ImportAction : AnAction() {

    // ! НИКАКОГО СОСТОЯНИЯ

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible = event.project != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(event: AnActionEvent) {
        if (event.project == null)
            return // no project

        val runManager: RunManager = RunManager.getInstance(event.project!!)
        val vscodeFolder: VirtualFile = event.project!!.guessProjectDir()?.findChild(".vscode") ?: return // no .vscode folder
        val launchFile: VirtualFile = FilenameIndex.getVirtualFilesByName("launch.json",
                GlobalSearchScopes.directoryScope(
                    event.project!!, vscodeFolder, false)
            ).first() ?: return // no launch.json file

        if (!launchFile.isValid)
            return // launch.json is invalid

        val content = String(launchFile.contentsToByteArray(), launchFile.charset)
        val json: JsonElement = try {Json.parseToJsonElement(content)} catch (exc: SerializationException) { return } // Might be bad JSON
        var config: RunnerAndConfigurationSettings
        for (currVSConfigElement: JsonElement in json.jsonObject["configurations"]!!.jsonArray) {
            val currVSConfig: JsonObject = currVSConfigElement.jsonObject

            if (currVSConfig["type"]?.jsonPrimitive?.content != "java")
                continue // not java run config

            val nameStr = currVSConfig["name"]?.jsonPrimitive?.content ?: throw ImportException("Config name is not specified")
            if (VariableRepository.getInstance().contains(nameStr))
                continue

            try {
                // & "request"
                when (currVSConfig["request"]?.jsonPrimitive?.content) {
                    "launch" -> {
        //                val sourcePaths = ???
        //                val JDK = ???
        //                val beforeRunTasks = appCfg.beforeRunTasks
        //                val classpathModifications = appCfg.classpathModifications
        //                val stepFilters = currVSConfig["stepFilters"]
        //                val inputRedirect = ???
        //                val suppressMultipleSessionWarning = ???

                        val javaAppCfgBuilder = JavaAppConfigBuilder(nameStr, event, currVSConfig)
                        config = javaAppCfgBuilder
                            .setMainClass()
                            .setJavaExec()
                            .setProgramArgs()
                            .setModulePaths()
                            .setClassPaths()
                            .setVMArgs()
                            .setWorkingDirectory()
                            .setEnv()
                            .setShortenCommandLine()
                            .setEncoding()
                            .build(runManager)
                    }
                    "attach" -> {
                        val remoteCfgBuilder = JavaRemoteConfigBuilder(nameStr, event, currVSConfig)
                        config = remoteCfgBuilder
                            .setHostName()
                            .setPort()
                            .build(runManager)
                    }
                    else -> {
            //                    Messages.showMessageDialog("Undefined configuration request type!", "Error", Messages.getErrorIcon());
                        throw ImportException("Undefined configuration request type!")
                    }
                }
                config.storeInDotIdeaFolder()
                runManager.addConfiguration(config)
                if (runManager.selectedConfiguration == null) {
                    runManager.selectedConfiguration = config
                }
                println("GOOD $nameStr")
            } catch (exc: ImportException) {
                println(exc.message)
            } catch (exc: RuntimeConfigurationException) {
                println(exc.message)
            } catch (exc: NullPointerException) {
                println(exc.message)
            }
        }
    }

}

package org.rri.ij_vscode_run_config

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.*
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScopes
import kotlinx.serialization.json.*


class ImportAction : AnAction() {

    // ! НИКАКОГО СОСТОЯНИЯ

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible = event.project != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(event: AnActionEvent) {
        if (event.project == null)
            return

        val vscodeFolder: VirtualFile = event.project!!.guessProjectDir()?.findChild(".vscode") ?: return
        val launchJson: JsonElement? = getJsonFromFolder(event, vscodeFolder, "launch.json")
        if (launchJson?.jsonObject?.get("configurations")?.jsonArray == null)
            return
        val settingsJson: JsonElement? = getJsonFromFolder(event, vscodeFolder, "settings.json")

        val runManager: RunManager = RunManager.getInstance(event.project!!)
        var config: RunnerAndConfigurationSettings
        for (currCfg: JsonElement in launchJson.jsonObject["configurations"]!!.jsonArray) {
            val cfgJson: JsonObject = currCfg.jsonObject
            if (cfgJson["type"]?.jsonPrimitive?.content != "java")
                continue

            val nameStr: String = cfgJson["name"]?.jsonPrimitive?.content ?: throw ImportException("Config name is not specified")
            if (VariableRepository.getInstance().contains(nameStr))
                continue

            try {
                when (cfgJson["request"]?.jsonPrimitive?.content) {
                    "launch" -> {
                        val javaAppCfgBuilder = JavaAppConfigBuilder(nameStr, event)
                        config = javaAppCfgBuilder
                            .setMainClass(cfgJson["mainClass"])
                            .setJavaExec(cfgJson["javaExec"] ?: settingsJson?.jsonObject?.get("java.configuration.runtimes"))
                            .setProgramArgs(cfgJson["args"])
                            .setModulePaths(cfgJson["modulePaths"])
                            .setClassPaths(cfgJson["classPaths"])
                            .setVMArgs(cfgJson["vmArgs"] ?: settingsJson?.jsonObject?.get("java.debug.settings.vmArgs"))
                            .setWorkingDirectory(cfgJson["cwd"])
                            .setEnv(cfgJson["env"])
                            .setEnvFromFile(cfgJson["envFile"])
                            .setShortenCommandLine(cfgJson["shortenCommandLine"])
                            .setEncoding(cfgJson["encoding"])
                            .build(runManager)
                    }
                    "attach" -> {
                        val remoteCfgBuilder = JavaRemoteConfigBuilder(nameStr, event)
                        config = remoteCfgBuilder
                            .setHostName(cfgJson["hostName"])
                            .setPort(cfgJson["port"])
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

    private fun getJsonFromFolder(event: AnActionEvent, vscodeFolder: VirtualFile, filename: String): JsonElement? {
        val file: VirtualFile? = FilenameIndex.getVirtualFilesByName(filename,
            GlobalSearchScopes.directoryScope(
                event.project!!, vscodeFolder, false)
        ).firstOrNull()

        if (file == null || !file.isValid)
            return null

        val content = String(file.contentsToByteArray(), file.charset)
        val json: JsonElement? = Json.runCatching { parseToJsonElement(content) }.getOrNull()

        return json?.jsonObject
    }

}

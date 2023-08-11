package org.rri.ij_vscode_run_config

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.RuntimeConfigurationException
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.serialization.json.*
import org.rri.ij_vscode_run_config.builders.JavaAppConfigBuilder
import org.rri.ij_vscode_run_config.builders.JavaRemoteConfigBuilder


class ImportConfigManager(private val project: Project, private val context: DataContext) {

    private val runManager = RunManager.getInstance(project)

    fun process() {
        val vscodeFolder: VirtualFile = project.guessProjectDir()?.findChild(".vscode")
            ?: return

        val launchElement: JsonElement? = getJsonFromFileInFolder(vscodeFolder, "launch.json")
        val launchJson = launchElement?.jsonObject?.get("configurations")?.jsonArray?.map { j -> j.jsonObject }
            ?: return
        val settingsJson: JsonObject? = getJsonFromFileInFolder(vscodeFolder, "settings.json")?.jsonObject

        for (cfgJson: JsonObject in launchJson) {
            if (cfgJson["type"]?.jsonPrimitive?.content != "java")
                continue

            val cfgName: String? = cfgJson["name"]?.jsonPrimitive?.content
            if (cfgName == null || VariableRepository.contains(cfgName))
                continue

//            try {
                val config = createJavaConfiguration(cfgName, cfgJson, settingsJson)

                config.storeInDotIdeaFolder()
                runManager.addConfiguration(config)

                if (runManager.selectedConfiguration == null) {
                    runManager.selectedConfiguration = config
                }

//            } catch (exc: ImportError) {
//                println(exc.message)
//            } catch (exc: ImportWarning){
//                println(exc.message)
//            } catch (exc: RuntimeConfigurationException) {
//                println(exc.message)
//            } catch (exc: NullPointerException) {
//                println(exc.message)
//            }
        }
    }

    private fun createJavaConfiguration(
        name: String,
        cfgJson: JsonObject,
        settingsJson: JsonObject?
    ): RunnerAndConfigurationSettings {
        val config: RunnerAndConfigurationSettings

        when (cfgJson["request"]?.jsonPrimitive?.content) {
            "launch" -> {
                val javaAppCfgBuilder = JavaAppConfigBuilder(name, project)
                config = javaAppCfgBuilder
                    .setMainClass(cfgJson["mainClass"])
                    .setJavaExec(cfgJson["javaExec"] ?: settingsJson?.get("java.configuration.runtimes"), context)
                    .setProgramArgs(cfgJson["args"])
                    .setModulePaths(cfgJson["modulePaths"], context)
                    .setClassPaths(cfgJson["classPaths"], context)
                    .setVMArgs(cfgJson["vmArgs"] ?: settingsJson?.get("java.debug.settings.vmArgs"))
                    .setWorkingDirectory(cfgJson["cwd"])
                    .setEnv(cfgJson["env"])
                    .setEnvFromFile(cfgJson["envFile"], context)
                    .setShortenCommandLine(cfgJson["shortenCommandLine"])
                    .setEncoding(cfgJson["encoding"])
                    .build(runManager)
            }

            "attach" -> {
                val remoteCfgBuilder = JavaRemoteConfigBuilder(name, project)
                config = remoteCfgBuilder
                    .setHostName(cfgJson["hostName"])
                    .setPort(cfgJson["port"])
                    .build(runManager)
            }

            else -> {
//                Messages.showMessageDialog("Undefined configuration request type!", "Error", Messages.getErrorIcon());
                throw ImportError("Undefined configuration request type!")
            }
        }

        return config
    }

    private fun getJsonFromFileInFolder(folder: VirtualFile, filename: String): JsonElement? {
        val file: VirtualFile? =
            LocalFileSystem.getInstance().findFileByIoFile(folder.toNioPath().resolve(filename).toFile())

        if (file == null || !file.isValid)
            return null

        val content = String(file.contentsToByteArray(), file.charset)
        val json: JsonElement? = Json.runCatching { parseToJsonElement(content) }.getOrNull()

        return json?.jsonObject
    }
}
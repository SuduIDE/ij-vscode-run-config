package org.rri.ij_vscode_run_config.logic.builders.tasks

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.runConfigurationType
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.sh.run.ShConfigurationType
import com.intellij.sh.run.ShRunConfiguration
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.rri.ij_vscode_run_config.detectShellPaths
import org.rri.ij_vscode_run_config.getStringFromJsonArrayOrString
import org.rri.ij_vscode_run_config.logic.ImportError
import org.rri.ij_vscode_run_config.logic.VariableRepository
import org.rri.ij_vscode_run_config.logic.builders.ConfigBuilderBase
import java.nio.file.Path

class ShellConfigBuilder(name: String, project: Project) : ConfigBuilderBase(name, project) {

    override val factory: ConfigurationFactory = runConfigurationType<ShConfigurationType>().configurationFactories[0]

    override val config: ShRunConfiguration =
        factory.createConfiguration(name, factory.createTemplateConfiguration(project)) as ShRunConfiguration

    init {
        config.isExecuteScriptFile = false
    }

    private val shells: List<String> = detectShellPaths()

    fun setCommand(value: JsonElement?, context: DataContext): ShellConfigBuilder {
        val content = value?.jsonPrimitive?.content ?: throw ImportError("Command is not specified")
        val command = VariableRepository.expandMacrosInString(content, context)

        // Check if command is a path to script
        config.isExecuteScriptFile = runCatching {
            FileUtil.isAbsolute(command) && FileUtil.exists(command)
        }.getOrElse { false }

        if (config.isExecuteScriptFile) {
            config.scriptPath = command
        } else {
            config.scriptText = command
        }

        return this
    }

    fun setWorkingDirectory(value: JsonElement?, context: DataContext): ShellConfigBuilder {
        val cwd = value?.jsonPrimitive?.content
        if (cwd != null) {
            config.scriptWorkingDirectory = VariableRepository.expandMacrosInString(cwd, context)
        }
//        else {
//            config.scriptWorkingDirectory = project.guessProjectDir()?.path
//        }

        return this
    }

    fun setEnv(value: JsonElement?, context: DataContext): ShellConfigBuilder {
        if (value != null) {
            val envMap: MutableMap<String, String> = HashMap()
            for (entry in value.jsonObject.entries) {
                envMap[entry.key] = VariableRepository.expandMacrosInString(entry.value.jsonPrimitive.content, context)
            }
            config.envData = EnvironmentVariablesData.create(envMap, true)
        }

        return this
    }

    fun setInterpreterPath(value: JsonElement?, context: DataContext) : ShellConfigBuilder {
        val content = value?.jsonPrimitive?.content
        if (content == null) {
            val interpreterPath = shells.stream().findFirst()
            if (!interpreterPath.isEmpty) {
                config.interpreterPath =  interpreterPath.get()
            }
            return this
        }

        val path = VariableRepository.expandMacrosInString(content, context)
        if (Path.of(path).isAbsolute && shells.contains(path)) {
            config.interpreterPath = path
        } else {
            config.interpreterPath = shells.stream().filter{shell -> shell.endsWith(path) || (path == "pws" && shell.contains("powershell"))}.findFirst().orElseThrow{ ImportError("Cannot find interpreter: $path")}
        }

        return this
    }

    fun setInterpreterOptions(value: JsonElement?, context: DataContext) : ShellConfigBuilder {
        val content = getStringFromJsonArrayOrString(value) ?: return this
        config.interpreterOptions = VariableRepository.expandMacrosInString(content, context)

        return this
    }

    fun setScriptOptions(value: JsonElement?, context: DataContext) : ShellConfigBuilder {
        val content = getStringFromJsonArrayOrString(value) ?: return this
        config.scriptOptions = VariableRepository.expandMacrosInString(content, context)

        return this
    }

    fun setIsTerminal(value: Boolean) : ShellConfigBuilder {
        config.isExecuteInTerminal = value

        return this
    }

}

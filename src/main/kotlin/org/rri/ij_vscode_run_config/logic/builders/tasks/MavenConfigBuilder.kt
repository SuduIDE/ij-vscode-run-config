package org.rri.ij_vscode_run_config.logic.builders.tasks

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.runConfigurationType
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.idea.maven.execution.MavenRunConfiguration
import org.jetbrains.idea.maven.execution.MavenRunConfigurationType
import org.rri.ij_vscode_run_config.getStringFromJsonArrayOrString
import org.rri.ij_vscode_run_config.logic.ImportError
import org.rri.ij_vscode_run_config.logic.VariableRepository
import org.rri.ij_vscode_run_config.logic.builders.ConfigBuilderBase


class MavenConfigBuilder(name: String, project: Project) : ConfigBuilderBase(name, project) {

    override val factory: ConfigurationFactory = runConfigurationType<MavenRunConfigurationType>().configurationFactories[0]

    override val config: MavenRunConfiguration = factory.createConfiguration(name, factory.createTemplateConfiguration(project)) as MavenRunConfiguration

    private var command: String? = null
    private var args: String? = null

    fun setCommandLine(value: JsonElement?, context: DataContext): MavenConfigBuilder {
        val content = value?.jsonPrimitive?.content ?: throw ImportError("Invalid Maven command")
        command = VariableRepository.expandMacrosInString(content, context)

        if (command!!.trim().startsWith("mvn")) {
            command = command?.trim()?.dropWhile{ c -> c != ' '}
        }

        return this
    }

    fun setArgs(value: JsonElement?, context: DataContext): MavenConfigBuilder {
        val content = getStringFromJsonArrayOrString(value) ?: return this
        args = VariableRepository.expandMacrosInString(content, context)

        return this
    }

    fun setWorkingDirectory(value: JsonElement?, context: DataContext): MavenConfigBuilder {
        val cwd = value?.jsonPrimitive?.content
        if (cwd != null) {
            config.runnerParameters.workingDirPath = VariableRepository.expandMacrosInString(cwd, context)
        } else {
            config.runnerParameters.workingDirPath = project.guessProjectDir()?.path!!
        }

        return this
    }

    fun setEnv(value: JsonElement?, context: DataContext): MavenConfigBuilder {
        if (value != null) {
            val envMap: MutableMap<String, String> = HashMap()
            for (entry in value.jsonObject.entries) {
                envMap[entry.key] = VariableRepository.expandMacrosInString(entry.value.jsonPrimitive.content, context)
            }
            config.runnerSettings?.environmentProperties = envMap
        }

        return this
    }

    override fun build(runManager: RunManager): RunnerAndConfigurationSettings {
        if (command == null)
            throw ImportError("Maven command is not specified")

        if (args == null) {
            config.runnerParameters.commandLine = command!!
        } else {
            config.runnerParameters.commandLine = "$command $args"
        }

        return super.build(runManager)
    }

}

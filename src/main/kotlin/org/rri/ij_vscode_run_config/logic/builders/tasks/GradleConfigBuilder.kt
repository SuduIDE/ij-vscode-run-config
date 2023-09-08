package org.rri.ij_vscode_run_config.logic.builders.tasks

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.runConfigurationType
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.plugins.gradle.service.execution.GradleExternalTaskConfigurationType
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration
import org.rri.ij_vscode_run_config.getStringFromJsonArrayOrString
import org.rri.ij_vscode_run_config.logic.ImportError
import org.rri.ij_vscode_run_config.logic.VariableRepository
import org.rri.ij_vscode_run_config.logic.builders.ConfigBuilderBase
import java.nio.file.Path


class GradleConfigBuilder(name: String, project: Project) : ConfigBuilderBase(name, project) {

    override val factory: ConfigurationFactory =
        runConfigurationType<GradleExternalTaskConfigurationType>().configurationFactories[0]

    override val config: GradleRunConfiguration =
        factory.createConfiguration(name, factory.createTemplateConfiguration(project)) as GradleRunConfiguration

    private var script: String? = null
    private var args: String? = null
    private var subProject: String? = null
    private var rootProjectName: String? = null
    private var projectFolderPath: String? = null

    fun setScript(value: JsonElement?, context: DataContext): GradleConfigBuilder {
        val content = value?.jsonPrimitive?.content ?: throw ImportError("Invalid gradle task")
        script = VariableRepository.expandMacrosInString(content, context)

        return this
    }

    fun setArgs(value: JsonElement?, context: DataContext): GradleConfigBuilder {
        val content = getStringFromJsonArrayOrString(value) ?: return this
        args = VariableRepository.expandMacrosInString(content, context)

        return this
    }

    fun setSubProject(value: JsonElement?, context: DataContext): GradleConfigBuilder {
        val content = value?.jsonPrimitive?.content ?: return this
        subProject = VariableRepository.expandMacrosInString(content, context)

        return this
    }

    fun setRootProject(value: JsonElement?, context: DataContext): GradleConfigBuilder {
        val content = value?.jsonPrimitive?.content ?: return this
        rootProjectName = VariableRepository.expandMacrosInString(content, context)

        return this
    }

    fun setProjectFolder(value: JsonElement?, context: DataContext): GradleConfigBuilder {
        val content = value?.jsonPrimitive?.content ?: return this
        projectFolderPath = VariableRepository.expandMacrosInString(content, context)

        return this
    }

    fun setJavaDebugFlag(value: Boolean): GradleConfigBuilder {
        config.isScriptDebugEnabled = value

        return this
    }

    fun setEnv(value: JsonElement?, context: DataContext): GradleConfigBuilder {
        if (value != null) {
            val envMap: MutableMap<String, String> = HashMap()
            for (entry in value.jsonObject.entries) {
                envMap[entry.key] = VariableRepository.expandMacrosInString(entry.value.jsonPrimitive.content, context)
            }
            config.settings.env = envMap
        }

        return this
    }

    override fun build(runManager: RunManager): RunnerAndConfigurationSettings {
        if (script == null)
            throw ImportError("Gradle command is not specified")

        if (args == null) {
            config.rawCommandLine = script!!
        } else {
            config.rawCommandLine = "$script $args"
        }

        if (projectFolderPath != null) {
            config.settings.externalProjectPath = projectFolderPath
        } else if (rootProjectName != null && rootProjectName == project.name) {
            config.settings.externalProjectPath = project.guessProjectDir()!!.path
        } else {
            throw ImportError("Gradle project is not specified")
        }

        if (subProject != null) {
            config.settings.externalProjectPath =
                Path.of(config.settings.externalProjectPath).resolve(subProject!!).toString()
        }

        return super.build(runManager)
    }
}
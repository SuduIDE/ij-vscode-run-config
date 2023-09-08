package org.rri.ij_vscode_run_config.logic.builders

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.JDOMUtil
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.text.StringUtil
import org.jdom.Element
import org.rri.ij_vscode_run_config.logic.PLUGIN_CONFIGURATION_NAME_SUFFIX
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

abstract class ConfigBuilderBase(protected var name: String, protected val project: Project) {

    protected abstract val factory: ConfigurationFactory
    protected abstract val config: RunConfiguration

    open fun build(runManager: RunManager): RunnerAndConfigurationSettings {
        config.checkConfiguration()

        if (runManager.findConfigurationByTypeAndName(factory.type, name) != null) {
            name = runManager.suggestUniqueName(name, factory.type)
            config.name = name
        }

        return runManager.createConfiguration(config, factory)
    }

}
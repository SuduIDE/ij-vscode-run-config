package org.rri.ij_vscode_run_config.builders

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.JDOMUtil
import org.jdom.Element
import org.rri.ij_vscode_run_config.ImportWarning
import org.rri.ij_vscode_run_config.VariableRepository

abstract class ConfigBuilderBase(protected var name: String, protected val project: Project) {

    protected abstract val factory: ConfigurationFactory
    protected abstract val config: RunConfiguration

    private fun hasSamePropertiesConfiguration(runManager: RunManager): Boolean {
        val thisElement = Element("configuration")
        config.writeExternal(thisElement)

        for (currentConfig in runManager.getConfigurationsList(factory.type)) {
            val thatElement = Element("configuration")
            currentConfig.writeExternal(thatElement)

            if (JDOMUtil.areElementsEqual(thisElement, thatElement)) {
                return true
            }
        }

        return false
    }

    fun build(runManager: RunManager): RunnerAndConfigurationSettings {
        config.checkConfiguration()

        if (hasSamePropertiesConfiguration(runManager))
            throw ImportWarning("Already have this configuration: $name")

        if (runManager.findConfigurationByTypeAndName(factory.type, name) != null) {
            name = runManager.suggestUniqueName(name, factory.type)
            config.name = name
        }

        return runManager.createConfiguration(config, factory)
    }

}
package org.rri.ij_vscode_run_config.logic.builders.configs

import com.intellij.execution.compound.CompoundRunConfiguration
import com.intellij.execution.compound.CompoundRunConfigurationType
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.runConfigurationType
import com.intellij.openapi.project.Project
import org.rri.ij_vscode_run_config.RunnableHolder
import org.rri.ij_vscode_run_config.logic.ImportError
import org.rri.ij_vscode_run_config.logic.builders.ConfigBuilderBase
import java.util.LinkedList

class CompoundConfigBuilder(name: String, project: Project) : ConfigBuilderBase(name, project) {

    override val factory: ConfigurationFactory =
        runConfigurationType<CompoundRunConfigurationType>().configurationFactories[0]

    override val config: CompoundRunConfiguration =
        factory.createConfiguration(name, factory.createTemplateConfiguration(project)) as CompoundRunConfiguration

    fun setConfigurations(value: Collection<String>, holders: Map<String, RunnableHolder>): CompoundConfigBuilder {
        if (value.isEmpty())
            throw ImportError("Invalid compound configuration")

        val configList: MutableList<RunConfiguration> = LinkedList()
        for (configName in value) {
            if (!holders.contains(configName)) {
                throw ImportError("Invalid configurations list")
            }
            configList.add(holders[configName]!!.settings.configuration)
        }

        config.setConfigurationsWithoutTargets(configList)
        return this
    }

}
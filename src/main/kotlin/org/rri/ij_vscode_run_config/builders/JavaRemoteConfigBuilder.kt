package org.rri.ij_vscode_run_config.builders

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.runConfigurationType
import com.intellij.execution.remote.RemoteConfiguration
import com.intellij.execution.remote.RemoteConfigurationType
import com.intellij.openapi.project.Project
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive

class JavaRemoteConfigBuilder(name: String, project: Project) : ConfigBuilderBase(name, project) {

    override val factory: ConfigurationFactory =
        runConfigurationType<RemoteConfigurationType>().configurationFactories[0]
    override val config: RemoteConfiguration = factory.createConfiguration(
        name,
        factory.createTemplateConfiguration(project)
    ) as RemoteConfiguration

    fun setHostName(value: JsonElement?): JavaRemoteConfigBuilder {
        val hostName: String? = value?.jsonPrimitive?.content
        if (hostName != null) {
            config.HOST = hostName
        }
        return this
    }

    fun setPort(value: JsonElement?): JavaRemoteConfigBuilder {
        val port: String? = value?.jsonPrimitive?.content
        if (port != null) {
            config.PORT = port
        }
        return this
    }

}
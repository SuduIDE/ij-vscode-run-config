package org.rri.ij_vscode_run_config

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.runConfigurationType
import com.intellij.execution.remote.RemoteConfiguration
import com.intellij.execution.remote.RemoteConfigurationType
import com.intellij.openapi.actionSystem.AnActionEvent
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class JavaRemoteConfigBuilder(private val name: String, private val event: AnActionEvent, private val json: JsonObject) {

    private val factory: ConfigurationFactory =
        runConfigurationType<RemoteConfigurationType>().configurationFactories[0]
    private val remoteCfg: RemoteConfiguration = factory.createConfiguration(
        name,
        factory.createTemplateConfiguration(event.project!!)
    ) as RemoteConfiguration


    fun setHostName(): JavaRemoteConfigBuilder {
        val hostName: String? = json["hostName"]?.jsonPrimitive?.content
        if (hostName != null) {
            remoteCfg.HOST = hostName
        }
        return this
    }

    fun setPort(): JavaRemoteConfigBuilder {
        val port: String? = json["port"]?.jsonPrimitive?.content
        if (port != null) {
            remoteCfg.PORT = port
        }
        return this
    }

    fun build(runManager: RunManager): RunnerAndConfigurationSettings {
        remoteCfg.checkConfiguration()
        return runManager.createConfiguration(remoteCfg, factory)
    }

}
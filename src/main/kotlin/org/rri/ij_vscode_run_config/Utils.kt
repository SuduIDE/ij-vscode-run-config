package org.rri.ij_vscode_run_config

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.text.StringUtil
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors

enum class DependsOrder {
    PARALLEL,
    SEQUENCE
}

class Dependencies(
    val all: MutableSet<String>,
    val onlyCurrent: MutableSet<String>,
    var dependsOrder: DependsOrder
) {
    fun deepCopy(): Dependencies {
        return Dependencies(all.toMutableSet(), onlyCurrent.toMutableSet(), dependsOrder)
    }
}

class RunnableHolder(
    var settings: RunnerAndConfigurationSettings,
    val dependencies: Dependencies
) {
    fun deepCopy(): RunnableHolder {
        return RunnableHolder(settings, dependencies.deepCopy())
    }
}

fun getStringFromJsonArrayOrString(value: JsonElement?): String? {
    return if (value.runCatching { value?.jsonPrimitive != null }.getOrElse { false }) {
        value?.jsonPrimitive?.content
    } else if (value?.jsonArray != null) {
        value.jsonArray.stream().map { m ->
            m.jsonPrimitive.content
        }.collect(Collectors.joining(" "))
    } else {
        null
    }
}

fun isMavenCommand(command: String?): Boolean {
    if (command == null)
        return false

    val isMvn = command.trim().startsWith("mvn ") || command.trim().startsWith("mvnw ")
    return isMvn && command.findAnyOf(listOf(";", "|", "||", "&", "&&")) == null
}

fun detectShellPaths(): List<String> {
    val shells: MutableList<String> = ArrayList()
    if (SystemInfo.isUnix) {
        addShellIfExists(shells, "/bin/bash")
        addShellIfExists(shells, "/usr/bin/bash")
        addShellIfExists(shells, "/usr/bin/zsh")
        addShellIfExists(shells, "/usr/local/bin/zsh")
        addShellIfExists(shells, "/usr/bin/fish")
        addShellIfExists(shells, "/usr/local/bin/fish")
    } else if (SystemInfo.isWindows) {
        val powershell = PathEnvironmentVariableUtil.findInPath("powershell.exe")
        if (powershell != null && StringUtil.startsWithIgnoreCase(
                powershell.absolutePath,
                "C:\\Windows\\System32\\WindowsPowerShell\\"
            )
        ) {
            shells.add(powershell.absolutePath)
        }
        val cmd = PathEnvironmentVariableUtil.findInPath("cmd.exe")
        if (cmd != null && StringUtil.startsWithIgnoreCase(cmd.absolutePath, "C:\\Windows\\System32\\")) {
            shells.add(cmd.absolutePath)
        }
        val pwsh = PathEnvironmentVariableUtil.findInPath("pwsh.exe")
        if (pwsh != null && StringUtil.startsWithIgnoreCase(pwsh.absolutePath, "C:\\Program Files\\PowerShell\\")) {
            shells.add(pwsh.absolutePath)
        }
        val gitBash = File("C:\\Program Files\\Git\\bin\\bash.exe")
        if (gitBash.isFile) {
            shells.add(gitBash.absolutePath)
        }
    }
    return shells
}

fun addShellIfExists(shells: MutableList<String>, filePath: String) {
    if (Files.exists(Path.of(filePath))) {
        shells.add(filePath)
    }
}

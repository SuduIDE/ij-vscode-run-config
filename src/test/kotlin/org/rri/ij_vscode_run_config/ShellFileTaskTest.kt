package org.rri.ij_vscode_run_config

import com.intellij.openapi.util.SystemInfo
import com.intellij.util.io.systemIndependentPath
import org.intellij.lang.annotations.Language
import org.rri.ij_vscode_run_config.logic.ImportManager
import java.io.File
import java.nio.file.Path

class ShellFileTaskTest : BaseImportTestCase() {

    private val scriptPath: Path = Path.of("scripts")
        .resolve(if (SystemInfo.isWindows) "Shell_file_task.ps1" else "Shell_file_task.sh")

    private val scriptPathJson: String = "${'$'}{workspaceFolder}${File.separator}$scriptPath".replace("\\", "\\\\")

    fun testShellFileTaskCommand() {
        setFileText(myTasksFile, getTasksJsonContentShellCommand())

        val importManager = ImportManager(project, myContext)
        importManager.deserialize()
        importManager.importAllConfigurations()

        assertSameFileWithText(getXmlOutputShellCommand(), getOutPath().resolve("Shell_file_task$configFileNameSuffix.xml"), false)
    }

    fun testShellFileTask() {
        setFileText(myTasksFile, getTasksJsonContentShellFile())

        val scriptsDir = createChildDirectory(myRoot, "scripts")
        val scriptFile = createChildData(scriptsDir, scriptPath.fileName.toString())
        setFileText(scriptFile, "echo 'Hello, world!'")

        val importManager = ImportManager(project, myContext)
        importManager.deserialize()
        importManager.importAllConfigurations()

        assertSameFileWithText(getXmlOutputShellFile(), getOutPath().resolve("Shell_file_task$configFileNameSuffix.xml"), false)
    }

    private fun getTasksJsonContentShellCommand(): String {
        @Language("JSON")
        val tasksJsonContentShellCommand: String = """
        {
            "version": "2.0.0",
            "tasks": [
                {
                    "label": "Shell file task",
                    "type": "shell",
                    "command": "echo",
                    "args": ["Hello,", "world!"],
                    "options": {
                        "cwd": "${'$'}{workspaceFolder}",
                        "env": {
                            "ONE": "ODIN",
                            "TWO": "DVA",
                            "THREE": "TRI"
                            },
                        "shell": {
                            "executable": "$defaultShell",
                            "args": ["-c"]
                        }
                    }
                }
            ]
        }
        """.trimIndent()

        return tasksJsonContentShellCommand
    }

    private fun getXmlOutputShellCommand(): String {
        @Language("XML")
        val xmlOutputShellCommand: String = """
        <component name="ProjectRunConfigurationManager">
          <configuration name="Shell file task$configNameSuffix" type="ShConfigurationType">
            <option name="SCRIPT_TEXT" value="echo" />
            <option name="INDEPENDENT_SCRIPT_PATH" value="true" />
            <option name="SCRIPT_PATH" value="" />
            <option name="SCRIPT_OPTIONS" value="Hello, world!" />
            <option name="INDEPENDENT_SCRIPT_WORKING_DIRECTORY" value="$workingDirectoryPathEqualsIndependent" />
            <option name="SCRIPT_WORKING_DIRECTORY" value="${'$'}PROJECT_DIR${'$'}" />
            <option name="INDEPENDENT_INTERPRETER_PATH" value="$interpreterPathEqualsIndependent" />
            <option name="INTERPRETER_PATH" value="$interpreterPath" />
            <option name="INTERPRETER_OPTIONS" value="-c" />
            <option name="EXECUTE_IN_TERMINAL" value="true" />
            <option name="EXECUTE_SCRIPT_FILE" value="false" />
            <envs>
              <env name="ONE" value="ODIN" />
              <env name="TWO" value="DVA" />
              <env name="THREE" value="TRI" />
            </envs>
            <method v="2" />
          </configuration>
        </component>
        """.trimIndent()

        return xmlOutputShellCommand
    }

    private fun getTasksJsonContentShellFile(): String {
        @Language("JSON")
        val tasksJsonContentShellFile: String = """
        {
            "version": "2.0.0",
            "tasks": [
                {
                    "label": "Shell file task",
                    "type": "shell",
                    "command": "$scriptPathJson",
                    "args": ["Hello,", "world!"]
                }
            ]
        }
        """.trimIndent()

        return tasksJsonContentShellFile
    }

    private fun getXmlOutputShellFile(): String {
        @Language("XML")
        val xmlOutputShellFile: String = """
        <component name="ProjectRunConfigurationManager">
          <configuration name="Shell file task$configNameSuffix" type="ShConfigurationType">
            <option name="SCRIPT_TEXT" value="" />
            <option name="INDEPENDENT_SCRIPT_PATH" value="false" />
            <option name="SCRIPT_PATH" value="${'$'}PROJECT_DIR${'$'}/${scriptPath.systemIndependentPath}" />
            <option name="SCRIPT_OPTIONS" value="Hello, world!" />
            <option name="INDEPENDENT_SCRIPT_WORKING_DIRECTORY" value="true" />
            <option name="SCRIPT_WORKING_DIRECTORY" value="${'$'}PROJECT_DIR${'$'}" />
            <option name="INDEPENDENT_INTERPRETER_PATH" value="$interpreterPathEqualsIndependent" />
            <option name="INTERPRETER_PATH" value="$interpreterPath" />
            <option name="INTERPRETER_OPTIONS" value="" />
            <option name="EXECUTE_IN_TERMINAL" value="true" />
            <option name="EXECUTE_SCRIPT_FILE" value="true" />
            <envs />
            <method v="2" />
          </configuration>
        </component>
        """.trimIndent()

        return xmlOutputShellFile
    }

}

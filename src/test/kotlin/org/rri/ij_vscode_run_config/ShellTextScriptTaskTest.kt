package org.rri.ij_vscode_run_config

import org.intellij.lang.annotations.Language
import org.rri.ij_vscode_run_config.logic.ImportManager

class ShellTextScriptTaskTest : BaseImportTestCase() {

    fun testShellTextScript() {
        setFileText(myTasksFile, getTasksJsonContent())

        val importManager = ImportManager(project, myContext)
        importManager.deserialize()
        importManager.importAllConfigurations()

        assertSameFileWithText(getXmlOutputShellCommand(), getOutPath().resolve("Shell_text_script$configFileNameSuffix.xml"), false)
    }

    private fun getTasksJsonContent(): String {
        @Language("JSON")
        val tasksJsonContent: String = """
        {
            "version": "2.0.0",
            "tasks": [
                {
                    "label": "Shell text script",
                    "type": "shell",
                    "command": "echo",
                    "args": ["Hello,", "world!"],
                    "options": {
                        "cwd": "${'$'}{workspaceFolder}",
                        "env": {
                            "ONE": "ODIN",
                            "TWO": "DVA",
                            "THREE": "TRI"
                        }
                    }
                }
            ]
        }
        """.trimIndent()

        return tasksJsonContent
    }

    private fun getXmlOutputShellCommand(): String {
        @Language("XML")
        val xmlOutputShellCommand: String = """
        <component name="ProjectRunConfigurationManager">
          <configuration name="Shell text script$configNameSuffix" type="ShConfigurationType">
            <option name="SCRIPT_TEXT" value="echo" />
            <option name="INDEPENDENT_SCRIPT_PATH" value="true" />
            <option name="SCRIPT_PATH" value="" />
            <option name="SCRIPT_OPTIONS" value="Hello, world!" />
            <option name="INDEPENDENT_SCRIPT_WORKING_DIRECTORY" value="$pathEqualsIndependent" />
            <option name="SCRIPT_WORKING_DIRECTORY" value="${'$'}PROJECT_DIR${'$'}" />
            <option name="INDEPENDENT_INTERPRETER_PATH" value="$pathEqualsIndependent" />
            <option name="INTERPRETER_PATH" value="$interpreterPath" />
            <option name="INTERPRETER_OPTIONS" value="" />
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

}

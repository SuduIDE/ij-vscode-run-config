package org.rri.ij_vscode_run_config

import org.intellij.lang.annotations.Language
import org.rri.ij_vscode_run_config.logic.ImportManager

class SimpleTaskTest : BaseImportTestCase() {

    fun testSimpleTaskShell() {
        setFileText(myTasksFile, getTasksJsonContentShell())

        val importManager = ImportManager(project, myContext)
        importManager.deserialize()
        importManager.importAllConfigurations()

        assertSameFileWithText(getXmlOutPutShell(), getOutPath().resolve("Simple_task$configFileNameSuffix.xml"), false)
    }

    fun testSimpleTaskProcess() {
        setFileText(myTasksFile, getTasksJsonContentProcess())

        val importManager = ImportManager(project, myContext)
        importManager.deserialize()
        importManager.importAllConfigurations()

        assertSameFileWithText(getXmlOutputProcess(), getOutPath().resolve("Simple_task$configFileNameSuffix.xml"), false)
    }

    private fun getTasksJsonContentShell(): String {
        @Language("JSON")
        val tasksJsonContentShell: String = """
        {
            "version": "2.0.0",
            "tasks": [
                {
                    "label": "Simple task",
                    "type": "shell",
                    "command": "echo",
                    "args": ["Hello, world!"]
                }
            ]
        }
        """.trimIndent()

        return tasksJsonContentShell
    }

    private fun getTasksJsonContentProcess(): String {
        @Language("JSON")
        val tasksJsonContentProcess: String = """
        {
            "version": "2.0.0",
            "tasks": [
                {
                    "label": "Simple task",
                    "type": "process",
                    "command": "echo",
                    "args": ["Hello, world!"]
                }
            ]
        }
        """.trimIndent()

        return tasksJsonContentProcess
    }


    private fun getXmlOutPutShell() : String {
        @Language("XML")
        val xmlOutputShell: String = """
        <component name="ProjectRunConfigurationManager">
          <configuration name="Simple task$configNameSuffix" type="ShConfigurationType">
            <option name="SCRIPT_TEXT" value="echo" />
            <option name="INDEPENDENT_SCRIPT_PATH" value="true" />
            <option name="SCRIPT_PATH" value="" />
            <option name="SCRIPT_OPTIONS" value="Hello, world!" />
            <option name="INDEPENDENT_SCRIPT_WORKING_DIRECTORY" value="true" />
            <option name="SCRIPT_WORKING_DIRECTORY" value="${'$'}PROJECT_DIR${'$'}" />
            <option name="INDEPENDENT_INTERPRETER_PATH" value="$interpreterPathEqualsIndependent" />
            <option name="INTERPRETER_PATH" value="$interpreterPath" />
            <option name="INTERPRETER_OPTIONS" value="" />
            <option name="EXECUTE_IN_TERMINAL" value="true" />
            <option name="EXECUTE_SCRIPT_FILE" value="false" />
            <envs />
            <method v="2" />
          </configuration>
        </component>
        """.trimIndent()

        return xmlOutputShell
    }


    private fun getXmlOutputProcess(): String {
        @Language("XML")
        val xmlOutputProcess: String = """
        <component name="ProjectRunConfigurationManager">
          <configuration name="Simple task$configNameSuffix" type="ShConfigurationType">
            <option name="SCRIPT_TEXT" value="echo" />
            <option name="INDEPENDENT_SCRIPT_PATH" value="true" />
            <option name="SCRIPT_PATH" value="" />
            <option name="SCRIPT_OPTIONS" value="Hello, world!" />
            <option name="INDEPENDENT_SCRIPT_WORKING_DIRECTORY" value="true" />
            <option name="SCRIPT_WORKING_DIRECTORY" value="${'$'}PROJECT_DIR${'$'}" />
            <option name="INDEPENDENT_INTERPRETER_PATH" value="$interpreterPathEqualsIndependent" />
            <option name="INTERPRETER_PATH" value="$interpreterPath" />
            <option name="INTERPRETER_OPTIONS" value="" />
            <option name="EXECUTE_IN_TERMINAL" value="false" />
            <option name="EXECUTE_SCRIPT_FILE" value="false" />
            <envs />
            <method v="2" />
          </configuration>
        </component>
        """.trimIndent()

        return xmlOutputProcess
    }



}

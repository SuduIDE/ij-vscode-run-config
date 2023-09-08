package org.rri.ij_vscode_run_config

import com.intellij.execution.RunManager
import org.intellij.lang.annotations.Language
import org.rri.ij_vscode_run_config.logic.ImportManager

class DependenciesTest : BaseImportTestCase() {

    fun testDependencies() {
        val runManager = RunManager.getInstance(project)

        setFileText(myLaunchFile, getLaunchJsonContent())
        setFileText(myTasksFile, getTasksJsonContent())

        assertEquals(0, runManager.allSettings.size)

        val importManager = ImportManager(project, myContext)
        importManager.deserialize()
        importManager.importAllConfigurations()

        assertEquals(23, runManager.allSettings.size)

        assertSameFileWithText(getXmlOutCompound(), getOutPath().resolve("Compound$configFileNameSuffix.xml"), false)
        assertSameFileWithText(getXmlOutFirst(), getOutPath().resolve("First$configFileNameSuffix.xml"), false)
        assertSameFileWithText(getXmlOutSecond(), getOutPath().resolve("Second$configFileNameSuffix.xml"), false)
        assertSameFileWithText(getXmlOutThird(), getOutPath().resolve("Third$configFileNameSuffix.xml"), false)

        assertSameFileWithText(getXmlOutputMainTaskCompound(), getOutPath().resolve("Main_task$configFileNameSuffix.xml"), false)
        assertSameFileWithText(getXmlOutputMainTaskSuffix(noDepsSuffix), getOutPath().resolve("Main_task$configFileNameSuffix$noDepsFileSuffix.xml"), false)

        assertSameFileWithText(getXmlOutputParallel_1(), getOutPath().resolve("Parallel_1$configFileNameSuffix.xml"), false)
        assertSameFileWithText(getXmlOutputParallel_2(), getOutPath().resolve("Parallel_2$configFileNameSuffix.xml"), false)
        assertSameFileWithText(getXmlOutputParallel_3_Compound(), getOutPath().resolve("Parallel_3$configFileNameSuffix.xml"), false)
        assertSameFileWithText(getXmlOutputParallel_3_Suffix(noDepsSuffix), getOutPath().resolve("Parallel_3$configFileNameSuffix$noDepsFileSuffix.xml"), false)

        assertSameFileWithText(getXmlOutputParallel_31(), getOutPath().resolve("Parallel_31$configFileNameSuffix.xml"), false)
        assertSameFileWithText(getXmlOutputParallel_32(), getOutPath().resolve("Parallel_32$configFileNameSuffix.xml"), false)

        assertSameFileWithText(getXmlOutputParallel_2111(), getOutPath().resolve("Parallel_2111$configFileNameSuffix.xml"), false)
        assertSameFileWithText(getXmlOutputParallel_2112(), getOutPath().resolve("Parallel_2112$configFileNameSuffix.xml"), false)

        assertSameFileWithText(getXmlOutputSequence_11(), getOutPath().resolve("Sequence_11$configFileNameSuffix.xml"), false)
        assertSameFileWithText(getXmlOutputSequence_12(), getOutPath().resolve("Sequence_12$configFileNameSuffix.xml"), false)
        assertSameFileWithText(getXmlOutputSequence_13(), getOutPath().resolve("Sequence_13$configFileNameSuffix.xml"), false)

        assertSameFileWithText(getXmlOutputSequence_21(), getOutPath().resolve("Sequence_21$configFileNameSuffix.xml"), false)
        assertSameFileWithText(getXmlOutputSequence_211_Compound(), getOutPath().resolve("Sequence_211$configFileNameSuffix.xml"), false)
        assertSameFileWithText(getXmlOutputSequence_211_Suffix(noDepsSuffix), getOutPath().resolve("Sequence_211$configFileNameSuffix$noDepsFileSuffix.xml"), false)
        assertSameFileWithText(getXmlOutputSequence_212(), getOutPath().resolve("Sequence_212$configFileNameSuffix.xml"), false)

        assertSameFileWithText(getXmlOutputSequence_311(), getOutPath().resolve("Sequence_311$configFileNameSuffix.xml"), false)
        assertSameFileWithText(getXmlOutputSequence_312(), getOutPath().resolve("Sequence_312$configFileNameSuffix.xml"), false)
    }

    private fun getLaunchJsonContent(): String {
        @Language("JSON")
        val launchFileContent: String = """
        {
            "version": "0.2.0",
            "compounds": [
                {
                    "name": "Compound",
                    "configurations": ["First", "Second", "Third"]
                }
            ],
            "configurations": [
                {
                    "type": "java",
                    "name": "First",
                    "request": "launch",
                    
                    "mainClass": "example.Main",
                    "cwd": "${'$'}{workspaceFolder}",
                    "args": "This is First config",
                    "env": {
                      "FIRST": "TRUE",
                      "SECOND": "FALSE",
                      "THIRD": "FALSE"
                    },
                    
                    "preLaunchTask": "Parallel_1"
                },
                {
                    "type": "java",
                    "name": "Second",
                    "request": "launch",
                    
                    "mainClass": "example.Main",
                    "cwd": "${'$'}{workspaceFolder}",
                    "args": "This is Second config",
                    "env": {
                      "FIRST": "FALSE",
                      "SECOND": "TRUE",
                      "THIRD": "FALSE"
                    },
                    
                    "preLaunchTask": "Parallel_2"
                },
                {
                    "type": "java",
                    "name": "Third",
                    "request": "launch",
                    
                    "mainClass": "example.Main",
                    "cwd": "${'$'}{workspaceFolder}",
                    "args": "This is Third config",
                    "env": {
                      "FIRST": "FALSE",
                      "SECOND": "FALSE",
                      "THIRD": "TRUE"
                    },
                    
                    "preLaunchTask": "Parallel_3"
                }
            ]
        }
        """.trimIndent()

        return launchFileContent
    }

    private fun getTasksJsonContent(): String {
        @Language("JSON")
        val tasksJsonContent: String = """
        {
            "version": "2.0.0",
            "tasks": [
                {
                    "label": "Main task",
                    "type": "shell",
                    "command": "echo main_task",
                    "dependsOn": ["Parallel_1", "Parallel_2", "Parallel_3"],
                    "dependsOrder": "parallel"
                },
                {
                    "label": "Parallel_1",
                    "type": "shell",
                    "command": "echo parallel_1",
                    "dependsOn": ["Sequence_11", "Sequence_12", "Sequence_13"],
                    "dependsOrder": "sequence"
                },
                {
                    "label": "Sequence_11",
                    "type": "shell",
                    "command": "echo sequence_11"
                },
                {
                    "label": "Sequence_12",
                    "type": "shell",
                    "command": "echo sequence_12"
                },
                {
                    "label": "Sequence_13",
                    "type": "shell",
                    "command": "echo sequence_13"
                },
                {
                    "label": "Parallel_2",
                    "type": "shell",
                    "command": "echo parallel_2",
                    "dependsOn": ["Sequence_21"],
                    "dependsOrder": "sequence"
                },
                {
                    "label": "Sequence_21",
                    "type": "shell",
                    "command": "echo sequence_21",
                    "dependsOn": ["Sequence_211", "Sequence_212"],
                    "dependsOrder": "sequence"
                },
                {
                    "label": "Sequence_211",
                    "type": "shell",
                    "command": "echo sequence_211",
                    "dependsOn": ["Parallel_2111", "Parallel_2112"],
                    "dependsOrder": "parallel"
                },
                {
                    "label": "Parallel_2111",
                    "type": "shell",
                    "command": "echo parallel_2111"
                },
                {
                    "label": "Parallel_2112",
                    "type": "shell",
                    "command": "echo parallel_2112"
                },
                {
                    "label": "Sequence_212",
                    "type": "shell",
                    "command": "echo sequence_212"
                },
                {
                    "label": "Parallel_3",
                    "type": "shell",
                    "command": "echo parallel_3",
                    "dependsOn": ["Parallel_31", "Parallel_32"],
                    "dependsOrder": "parallel"
                },
                {
                    "label": "Parallel_31",
                    "type": "shell",
                    "command": "echo parallel_31",
                    "dependsOn": ["Sequence_311", "Sequence_312"],
                    "dependsOrder": "sequence"
                },
                {
                    "label": "Sequence_311",
                    "type": "shell",
                    "command": "echo sequence_311"
                },
                {
                    "label": "Sequence_312",
                    "type": "shell",
                    "command": "echo sequence_312"
                },
                {
                    "label": "Parallel_32",
                    "type": "shell",
                    "command": "echo parallel_32"
                }
            ]
        }
        """.trimIndent()

        return tasksJsonContent
    }

    private fun getXmlOutputMainTaskCompound(): String {
        @Language("XML")
        val xmlOutputMainTask: String = """
        <component name="ProjectRunConfigurationManager">
          <configuration name="Main task$configNameSuffix" type="CompoundRunConfigurationType">
            <toRun name="Parallel_1$configNameSuffix" type="ShConfigurationType" />
            <toRun name="Parallel_2$configNameSuffix" type="ShConfigurationType" />
            <toRun name="Parallel_3$configNameSuffix" type="CompoundRunConfigurationType" />
            <toRun name="Main task$configNameSuffix$noDepsSuffix" type="ShConfigurationType" />
            <method v="2" />
          </configuration>
        </component>
        """.trimIndent()

        return xmlOutputMainTask
    }

    private fun getXmlOutputMainTaskSuffix(suffix: String): String {
        @Language("XML")
        val xmlOutputMainTaskNoDeps: String = """
        <component name="ProjectRunConfigurationManager">
          <configuration name="Main task$configNameSuffix$suffix" type="ShConfigurationType">
            <option name="SCRIPT_TEXT" value="echo main_task" />
            <option name="INDEPENDENT_SCRIPT_PATH" value="true" />
            <option name="SCRIPT_PATH" value="" />
            <option name="SCRIPT_OPTIONS" value="" />
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

        return xmlOutputMainTaskNoDeps
    }

    private fun getXmlOutputParallel_1(): String {
        @Language("XML")
        val xmlOutputParallel_1: String = """
            <component name="ProjectRunConfigurationManager">
              <configuration name="Parallel_1$configNameSuffix" type="ShConfigurationType">
                <option name="SCRIPT_TEXT" value="echo parallel_1" />
                <option name="INDEPENDENT_SCRIPT_PATH" value="true" />
                <option name="SCRIPT_PATH" value="" />
                <option name="SCRIPT_OPTIONS" value="" />
                <option name="INDEPENDENT_SCRIPT_WORKING_DIRECTORY" value="true" />
                <option name="SCRIPT_WORKING_DIRECTORY" value="${'$'}PROJECT_DIR${'$'}" />
                <option name="INDEPENDENT_INTERPRETER_PATH" value="$interpreterPathEqualsIndependent" />
                <option name="INTERPRETER_PATH" value="$interpreterPath" />
                <option name="INTERPRETER_OPTIONS" value="" />
                <option name="EXECUTE_IN_TERMINAL" value="true" />
                <option name="EXECUTE_SCRIPT_FILE" value="false" />
                <envs />
                <method v="2">
                  <option name="RunConfigurationTask" enabled="false" run_configuration_name="Sequence_11$configNameSuffix" run_configuration_type="ShConfigurationType" />
                  <option name="RunConfigurationTask" enabled="false" run_configuration_name="Sequence_12$configNameSuffix" run_configuration_type="ShConfigurationType" />
                  <option name="RunConfigurationTask" enabled="false" run_configuration_name="Sequence_13$configNameSuffix" run_configuration_type="ShConfigurationType" />
                </method>
              </configuration>
            </component>
            """.trimIndent()

        return xmlOutputParallel_1
    }

    private fun getXmlOutputParallel_2(): String {
        @Language("XML")
        val xmlOutputParallel_2: String = """
        <component name="ProjectRunConfigurationManager">
          <configuration name="Parallel_2$configNameSuffix" type="ShConfigurationType">
            <option name="SCRIPT_TEXT" value="echo parallel_2" />
            <option name="INDEPENDENT_SCRIPT_PATH" value="true" />
            <option name="SCRIPT_PATH" value="" />
            <option name="SCRIPT_OPTIONS" value="" />
            <option name="INDEPENDENT_SCRIPT_WORKING_DIRECTORY" value="true" />
            <option name="SCRIPT_WORKING_DIRECTORY" value="${'$'}PROJECT_DIR${'$'}" />
            <option name="INDEPENDENT_INTERPRETER_PATH" value="$interpreterPathEqualsIndependent" />
            <option name="INTERPRETER_PATH" value="$interpreterPath" />
            <option name="INTERPRETER_OPTIONS" value="" />
            <option name="EXECUTE_IN_TERMINAL" value="true" />
            <option name="EXECUTE_SCRIPT_FILE" value="false" />
            <envs />
            <method v="2">
              <option name="RunConfigurationTask" enabled="false" run_configuration_name="Sequence_21$configNameSuffix" run_configuration_type="ShConfigurationType" />
            </method>
          </configuration>
        </component>
        """.trimIndent()

        return xmlOutputParallel_2
    }

    private fun getXmlOutputParallel_3_Compound(): String {
        @Language("XML")
        val xmlOutputParallel_3: String = """
        <component name="ProjectRunConfigurationManager">
          <configuration name="Parallel_3$configNameSuffix" type="CompoundRunConfigurationType">
            <toRun name="Parallel_31$configNameSuffix" type="ShConfigurationType" />
            <toRun name="Parallel_32$configNameSuffix" type="ShConfigurationType" />
            <toRun name="Parallel_3$configNameSuffix$noDepsSuffix" type="ShConfigurationType" />
            <method v="2" />
          </configuration>
        </component>
        """.trimIndent()

        return xmlOutputParallel_3
    }

    private fun getXmlOutputParallel_3_Suffix(suffix: String): String {
        @Language("XML")
        val xmlOutputParallel_3_NoDeps: String = """
        <component name="ProjectRunConfigurationManager">
          <configuration name="Parallel_3$configNameSuffix$suffix" type="ShConfigurationType">
            <option name="SCRIPT_TEXT" value="echo parallel_3" />
            <option name="INDEPENDENT_SCRIPT_PATH" value="true" />
            <option name="SCRIPT_PATH" value="" />
            <option name="SCRIPT_OPTIONS" value="" />
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

        return xmlOutputParallel_3_NoDeps
    }

    private fun getXmlOutputParallel_31(): String {
        @Language("XML")
        val xmlOutputParallel_31: String = """
        <component name="ProjectRunConfigurationManager">
          <configuration name="Parallel_31$configNameSuffix" type="ShConfigurationType">
            <option name="SCRIPT_TEXT" value="echo parallel_31" />
            <option name="INDEPENDENT_SCRIPT_PATH" value="true" />
            <option name="SCRIPT_PATH" value="" />
            <option name="SCRIPT_OPTIONS" value="" />
            <option name="INDEPENDENT_SCRIPT_WORKING_DIRECTORY" value="true" />
            <option name="SCRIPT_WORKING_DIRECTORY" value="${'$'}PROJECT_DIR${'$'}" />
            <option name="INDEPENDENT_INTERPRETER_PATH" value="$interpreterPathEqualsIndependent" />
            <option name="INTERPRETER_PATH" value="$interpreterPath" />
            <option name="INTERPRETER_OPTIONS" value="" />
            <option name="EXECUTE_IN_TERMINAL" value="true" />
            <option name="EXECUTE_SCRIPT_FILE" value="false" />
            <envs />
            <method v="2">
              <option name="RunConfigurationTask" enabled="false" run_configuration_name="Sequence_311$configNameSuffix" run_configuration_type="ShConfigurationType" />
              <option name="RunConfigurationTask" enabled="false" run_configuration_name="Sequence_312$configNameSuffix" run_configuration_type="ShConfigurationType" />
            </method>
          </configuration>
        </component>
        """.trimIndent()

        return xmlOutputParallel_31
    }

    private fun getXmlOutputParallel_32(): String {
        @Language("XML")
        val xmlOutputParallel_32: String = """
        <component name="ProjectRunConfigurationManager">
          <configuration name="Parallel_32$configNameSuffix" type="ShConfigurationType">
            <option name="SCRIPT_TEXT" value="echo parallel_32" />
            <option name="INDEPENDENT_SCRIPT_PATH" value="true" />
            <option name="SCRIPT_PATH" value="" />
            <option name="SCRIPT_OPTIONS" value="" />
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

        return xmlOutputParallel_32
    }

    private fun getXmlOutputParallel_2111(): String {
        @Language("XML")
        val xmlOutputParallel_2111: String = """
        <component name="ProjectRunConfigurationManager">
          <configuration name="Parallel_2111$configNameSuffix" type="ShConfigurationType">
            <option name="SCRIPT_TEXT" value="echo parallel_2111" />
            <option name="INDEPENDENT_SCRIPT_PATH" value="true" />
            <option name="SCRIPT_PATH" value="" />
            <option name="SCRIPT_OPTIONS" value="" />
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

        return xmlOutputParallel_2111
    }

    private fun getXmlOutputParallel_2112(): String {
        @Language("XML")
        val xmlOutputParallel_2112: String = """
        <component name="ProjectRunConfigurationManager">
          <configuration name="Parallel_2112$configNameSuffix" type="ShConfigurationType">
            <option name="SCRIPT_TEXT" value="echo parallel_2112" />
            <option name="INDEPENDENT_SCRIPT_PATH" value="true" />
            <option name="SCRIPT_PATH" value="" />
            <option name="SCRIPT_OPTIONS" value="" />
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

        return xmlOutputParallel_2112
    }

    private fun getXmlOutputSequence_11(): String {
        @Language("XML")
        val xmlOutputSequence_11: String = """
        <component name="ProjectRunConfigurationManager">
          <configuration name="Sequence_11$configNameSuffix" type="ShConfigurationType">
            <option name="SCRIPT_TEXT" value="echo sequence_11" />
            <option name="INDEPENDENT_SCRIPT_PATH" value="true" />
            <option name="SCRIPT_PATH" value="" />
            <option name="SCRIPT_OPTIONS" value="" />
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

        return xmlOutputSequence_11
    }

    private fun getXmlOutputSequence_12(): String {
        @Language("XML")
        val xmlOutputSequence_12: String = """
        <component name="ProjectRunConfigurationManager">
          <configuration name="Sequence_12$configNameSuffix" type="ShConfigurationType">
            <option name="SCRIPT_TEXT" value="echo sequence_12" />
            <option name="INDEPENDENT_SCRIPT_PATH" value="true" />
            <option name="SCRIPT_PATH" value="" />
            <option name="SCRIPT_OPTIONS" value="" />
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

        return xmlOutputSequence_12
    }

    private fun getXmlOutputSequence_13(): String {
        @Language("XML")
        val xmlOutputSequence_13: String = """
        <component name="ProjectRunConfigurationManager">
          <configuration name="Sequence_13$configNameSuffix" type="ShConfigurationType">
            <option name="SCRIPT_TEXT" value="echo sequence_13" />
            <option name="INDEPENDENT_SCRIPT_PATH" value="true" />
            <option name="SCRIPT_PATH" value="" />
            <option name="SCRIPT_OPTIONS" value="" />
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

        return xmlOutputSequence_13
    }

    private fun getXmlOutputSequence_21(): String {
        @Language("XML")
        val xmlOutputSequence_21: String = """
        <component name="ProjectRunConfigurationManager">
          <configuration name="Sequence_21$configNameSuffix" type="ShConfigurationType">
            <option name="SCRIPT_TEXT" value="echo sequence_21" />
            <option name="INDEPENDENT_SCRIPT_PATH" value="true" />
            <option name="SCRIPT_PATH" value="" />
            <option name="SCRIPT_OPTIONS" value="" />
            <option name="INDEPENDENT_SCRIPT_WORKING_DIRECTORY" value="true" />
            <option name="SCRIPT_WORKING_DIRECTORY" value="${'$'}PROJECT_DIR${'$'}" />
            <option name="INDEPENDENT_INTERPRETER_PATH" value="$interpreterPathEqualsIndependent" />
            <option name="INTERPRETER_PATH" value="$interpreterPath" />
            <option name="INTERPRETER_OPTIONS" value="" />
            <option name="EXECUTE_IN_TERMINAL" value="true" />
            <option name="EXECUTE_SCRIPT_FILE" value="false" />
            <envs />
            <method v="2">
              <option name="RunConfigurationTask" enabled="false" run_configuration_name="Sequence_211$configNameSuffix" run_configuration_type="CompoundRunConfigurationType" />
              <option name="RunConfigurationTask" enabled="false" run_configuration_name="Sequence_212$configNameSuffix" run_configuration_type="ShConfigurationType" />
            </method>
          </configuration>
        </component>
        """.trimIndent()

        return xmlOutputSequence_21
    }

    private fun getXmlOutputSequence_211_Compound(): String {
        @Language("XML")
        val xmlOutputSequence_211: String = """
        <component name="ProjectRunConfigurationManager">
          <configuration name="Sequence_211$configNameSuffix" type="CompoundRunConfigurationType">
            <toRun name="Parallel_2111$configNameSuffix" type="ShConfigurationType" />
            <toRun name="Parallel_2112$configNameSuffix" type="ShConfigurationType" />
            <toRun name="Sequence_211$configNameSuffix$noDepsSuffix" type="ShConfigurationType" />
            <method v="2" />
          </configuration>
        </component>
        """.trimIndent()

        return xmlOutputSequence_211
    }

    private fun getXmlOutputSequence_211_Suffix(suffix: String): String {
        @Language("XML")
        val xmlOutputSequence_211_NoDeps: String = """
        <component name="ProjectRunConfigurationManager">
          <configuration name="Sequence_211$configNameSuffix$suffix" type="ShConfigurationType">
            <option name="SCRIPT_TEXT" value="echo sequence_211" />
            <option name="INDEPENDENT_SCRIPT_PATH" value="true" />
            <option name="SCRIPT_PATH" value="" />
            <option name="SCRIPT_OPTIONS" value="" />
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

        return xmlOutputSequence_211_NoDeps
    }

    private fun getXmlOutputSequence_212(): String {
        @Language("XML")
        val xmlOutputSequence_212: String = """
        <component name="ProjectRunConfigurationManager">
          <configuration name="Sequence_212$configNameSuffix" type="ShConfigurationType">
            <option name="SCRIPT_TEXT" value="echo sequence_212" />
            <option name="INDEPENDENT_SCRIPT_PATH" value="true" />
            <option name="SCRIPT_PATH" value="" />
            <option name="SCRIPT_OPTIONS" value="" />
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

        return xmlOutputSequence_212
    }

    private fun getXmlOutputSequence_311(): String {
        @Language("XML")
        val xmlOutputSequence_311: String = """
        <component name="ProjectRunConfigurationManager">
          <configuration name="Sequence_311$configNameSuffix" type="ShConfigurationType">
            <option name="SCRIPT_TEXT" value="echo sequence_311" />
            <option name="INDEPENDENT_SCRIPT_PATH" value="true" />
            <option name="SCRIPT_PATH" value="" />
            <option name="SCRIPT_OPTIONS" value="" />
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

        return xmlOutputSequence_311
    }

    private fun getXmlOutputSequence_312(): String {
        @Language("XML")
        val xmlOutputSequence_312: String = """
        <component name="ProjectRunConfigurationManager">
          <configuration name="Sequence_312$configNameSuffix" type="ShConfigurationType">
            <option name="SCRIPT_TEXT" value="echo sequence_312" />
            <option name="INDEPENDENT_SCRIPT_PATH" value="true" />
            <option name="SCRIPT_PATH" value="" />
            <option name="SCRIPT_OPTIONS" value="" />
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

        return xmlOutputSequence_312
    }

    private fun getXmlOutCompound(): String {
        @Language("XML")
        val xmlOutputCompound: String = """
        <component name="ProjectRunConfigurationManager">
          <configuration name="Compound$configNameSuffix" type="CompoundRunConfigurationType">
            <toRun name="First$configNameSuffix" type="Application" />
            <toRun name="Second$configNameSuffix" type="Application" />
            <toRun name="Third$configNameSuffix" type="Application" />
            <method v="2" />
          </configuration>
        </component>
        """.trimIndent()

        return xmlOutputCompound
    }

    private fun getXmlOutFirst(): String {
        @Language("XML")
        val xmlOutputFirst: String = """
        <component name="ProjectRunConfigurationManager">
          <configuration name="First$configNameSuffix" type="Application" factoryName="Application">
            <envs>
              <env name="THIRD" value="FALSE" />
              <env name="SECOND" value="FALSE" />
              <env name="FIRST" value="TRUE" />
            </envs>
            <option name="MAIN_CLASS_NAME" value="example.Main" />
            <module name="VSCode_Import_Run_Config_Test" />
            <option name="PROGRAM_PARAMETERS" value="This is First config" />
            <option name="WORKING_DIRECTORY" value="${'$'}ProjectFileDir${'$'}" />
            <method v="2">
              <option name="RunConfigurationTask" enabled="false" run_configuration_name="Parallel_1$configNameSuffix" run_configuration_type="ShConfigurationType" />
            </method>
          </configuration>
        </component>
        """.trimIndent()

        return xmlOutputFirst
    }

    private fun getXmlOutSecond(): String {
        @Language("XML")
        val xmlOutputSecond: String = """
        <component name="ProjectRunConfigurationManager">
          <configuration name="Second$configNameSuffix" type="Application" factoryName="Application">
            <envs>
              <env name="THIRD" value="FALSE" />
              <env name="SECOND" value="TRUE" />
              <env name="FIRST" value="FALSE" />
            </envs>
            <option name="MAIN_CLASS_NAME" value="example.Main" />
            <module name="VSCode_Import_Run_Config_Test" />
            <option name="PROGRAM_PARAMETERS" value="This is Second config" />
            <option name="WORKING_DIRECTORY" value="${'$'}ProjectFileDir${'$'}" />
            <method v="2">
              <option name="RunConfigurationTask" enabled="false" run_configuration_name="Parallel_2$configNameSuffix" run_configuration_type="ShConfigurationType" />
            </method>
          </configuration>
        </component>
        """.trimIndent()

        return xmlOutputSecond
    }

    private fun getXmlOutThird(): String {
        @Language("XML")
        val xmlOutputThird: String = """
        <component name="ProjectRunConfigurationManager">
          <configuration name="Third$configNameSuffix" type="Application" factoryName="Application">
            <envs>
              <env name="THIRD" value="TRUE" />
              <env name="SECOND" value="FALSE" />
              <env name="FIRST" value="FALSE" />
            </envs>
            <option name="MAIN_CLASS_NAME" value="example.Main" />
            <module name="VSCode_Import_Run_Config_Test" />
            <option name="PROGRAM_PARAMETERS" value="This is Third config" />
            <option name="WORKING_DIRECTORY" value="${'$'}ProjectFileDir${'$'}" />
            <method v="2">
              <option name="RunConfigurationTask" enabled="false" run_configuration_name="Parallel_3$configNameSuffix" run_configuration_type="CompoundRunConfigurationType" />
            </method>
          </configuration>
        </component>
        """.trimIndent()

        return xmlOutputThird
    }

}

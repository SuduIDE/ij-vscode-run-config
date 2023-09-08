package org.rri.ij_vscode_run_config

import com.intellij.execution.RunManager
import org.intellij.lang.annotations.Language
import org.rri.ij_vscode_run_config.logic.ImportManager

class MultipleConfigsTest : BaseImportTestCase() {

    fun testMultipleConfigs() {
        setFileText(myLaunchFile, launchJsonContent)

        val runManager = RunManager.getInstance(project)

        assertEquals(0, runManager.allSettings.size)

        val importManager = ImportManager(project, myContext)
        importManager.deserialize()
        importManager.importAllConfigurations()

        assertEquals(4, runManager.allSettings.size)
        assertSameFileWithText(xmlOutputFirst, getOutPath().resolve("First$configFileNameSuffix.xml"))
        assertSameFileWithText(xmlOutputSecond, getOutPath().resolve("Second$configFileNameSuffix.xml"))
        assertSameFileWithText(xmlOutputThird, getOutPath().resolve("Third$configFileNameSuffix.xml"))
        assertSameFileWithText(xmlOutputFourth, getOutPath().resolve("Fourth__remote_$configFileNameSuffix.xml"))
    }

    @Language("JSON")
    private val launchJsonContent: String = """
        {
            "version": "0.2.0",
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
                    }
                },
                {
                  "name": "C++ Launch",
                  "type": "cppdbg",
                  "request": "launch",
                  "program": "${'$'}{workspaceFolder}/a.out",
                  "args": ["arg1", "arg2"],
                  "environment": [{ "name": "config", "value": "Debug" }],
                  "cwd": "${'$'}{workspaceFolder}"
                },
                {
                  "name": "Python Launch",
                  "type": "python",
                  "request": "launch",
                  "program": "${'$'}{file}",
                  "console": "integratedTerminal",
                  "justMyCode": true
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
                    }
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
                    }
                },
                {
                  "type": "node",
                  "request": "launch",
                  "name": "Launch Program",
                  "program": "${'$'}{workspaceFolder}/helloworld.ts",
                  "preLaunchTask": "tsc: build - tsconfig.json",
                  "outFiles": ["${'$'}{workspaceFolder}/out/**/*.js"]
                },
                {
                    "type": "java",
                    "name": "Fourth (remote)",
                    "request": "attach",
                    
                    "hostName": "12.34.56.78",
                    "port": 12345
                }
            ]
        }
        """.trimIndent()

    @Language("XML")
    private val xmlOutputFirst: String = """
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
              <option name="Make" enabled="true" />
            </method>
          </configuration>
        </component>
        """.trimIndent()

    @Language("XML")
    private val xmlOutputSecond: String = """
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
              <option name="Make" enabled="true" />
            </method>
          </configuration>
        </component>
        """.trimIndent()

    @Language("XML")
    private val xmlOutputThird: String = """
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
              <option name="Make" enabled="true" />
            </method>
          </configuration>
        </component>
        """.trimIndent()

    @Language("XML")
    private val xmlOutputFourth: String = """
        <component name="ProjectRunConfigurationManager">
          <configuration name="Fourth (remote)$configNameSuffix" type="Remote">
            <option name="USE_SOCKET_TRANSPORT" value="true" />
            <option name="SERVER_MODE" value="false" />
            <option name="SHMEM_ADDRESS" value="javadebug" />
            <option name="HOST" value="12.34.56.78" />
            <option name="PORT" value="12345" />
            <option name="AUTO_RESTART" value="false" />
            <method v="2" />
          </configuration>
        </component>
        """.trimIndent()

}

package org.rri.ij_vscode_run_config

import org.intellij.lang.annotations.Language
import org.rri.ij_vscode_run_config.logic.ImportManager


class JavaAppSimpleConfigTest : BaseImportTestCase() {

    fun testJavaAppSimpleConfig() {
        setFileText(myLaunchFile, getLaunchJsonContent())

        val importManager = ImportManager(project, myContext)
        importManager.deserialize()
        importManager.importAllConfigurations()

        assertSameFileWithText(xmlOutput, getOutPath().resolve("Simple$configFileNameSuffix.xml"))
    }

    @Language("XML")
    private val xmlOutput: String = """
        <component name="ProjectRunConfigurationManager">
          <configuration name="Simple$configNameSuffix" type="Application" factoryName="Application">
            <option name="MAIN_CLASS_NAME" value="example.Main" />
            <module name="VSCode_Import_Run_Config_Test" />
            <option name="WORKING_DIRECTORY" value="${'$'}ProjectFileDir${'$'}" />
            <method v="2">
              <option name="Make" enabled="true" />
            </method>
          </configuration>
        </component>
        """.trimIndent()

    private fun getLaunchJsonContent(): String {
        @Language("JSON")
        val launchContent: String = """
        {
            "version": "0.2.0",
            "configurations": [
                {
                    "type": "java",
                    "name": "Simple",
                    "request": "launch",
                    
                    "mainClass": "${myRoot.path}/src/example/Main.java",
                    "cwd": "${'$'}{workspaceFolder}"
                }
            ]
        }
        """.trimIndent()

        return launchContent
    }

}

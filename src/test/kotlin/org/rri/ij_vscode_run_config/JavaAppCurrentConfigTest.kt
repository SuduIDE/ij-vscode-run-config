package org.rri.ij_vscode_run_config

import com.intellij.execution.RunManager
import org.intellij.lang.annotations.Language
import org.rri.ij_vscode_run_config.logic.ImportManager

class JavaAppCurrentConfigTest : BaseImportTestCase() {

    fun testJavaAppCurrentConfig() {
        val runManager = RunManager.getInstance(project)

        setFileText(myLaunchFile, launchJsonContent)

        assertEmpty(runManager.allSettings)
        assertNull(runManager.selectedConfiguration)

        val importManager = ImportManager(project, myContext)
        importManager.deserialize()
        importManager.importAllConfigurations()

        assertSameFileWithText(xmlOutput, getOutPath().resolve("Current$configFileNameSuffix.xml"))
        assertNotEmpty(runManager.allSettings)
        assertNotNull(runManager.selectedConfiguration)
        assertEquals("Current$configNameSuffix", runManager.selectedConfiguration!!.name)
    }

    @Language("JSON")
    private val launchJsonContent: String = """
        {
            "version": "0.2.0",
            "configurations": [
                {
                    "type": "java",
                    "name": "Current",
                    "request": "launch",
                    
                    "mainClass": "example.Main",
                    "cwd": "${'$'}{workspaceFolder}"
                }
            ]
        }
        """.trimIndent()

    @Language("XML")
    private val xmlOutput: String = """
        <component name="ProjectRunConfigurationManager">
          <configuration name="Current$configNameSuffix" type="Application" factoryName="Application">
            <option name="MAIN_CLASS_NAME" value="example.Main" />
            <module name="VSCode_Import_Run_Config_Test" />
            <option name="WORKING_DIRECTORY" value="${'$'}ProjectFileDir${'$'}" />
            <method v="2">
              <option name="Make" enabled="true" />
            </method>
          </configuration>
        </component>
        """.trimIndent()

}

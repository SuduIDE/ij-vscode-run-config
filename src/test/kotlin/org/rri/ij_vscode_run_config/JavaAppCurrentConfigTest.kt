package org.rri.ij_vscode_run_config

import com.intellij.execution.RunManager
import org.intellij.lang.annotations.Language

class JavaAppCurrentConfigTest : BaseImportTestCase() {

    @Language("JSON")
    private val launchFileContent: String = """
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
          <configuration name="Current" type="Application" factoryName="Application">
            <option name="MAIN_CLASS_NAME" value="example.Main" />
            <module name="VSCode_Import_Run_Config_Test" />
            <option name="WORKING_DIRECTORY" value="${'$'}ProjectFileDir${'$'}" />
            <method v="2">
              <option name="Make" enabled="true" />
            </method>
          </configuration>
        </component>
        """.trimIndent()

    fun testJavaAppCurrentConfig() {
        setFileText(myLaunchFile, launchFileContent)

        val runManager = RunManager.getInstance(project)

        assertEmpty(runManager.allSettings)
        assertNull(runManager.selectedConfiguration)

        val importConfigManager = ImportConfigManager(project, myContext)
        importConfigManager.process()

        assertSameFileWithText(xmlOutput, getOutPath().resolve("Current.xml"))
        assertNotEmpty(runManager.allSettings)
        assertNotNull(runManager.selectedConfiguration)
        assertEquals("Current", runManager.selectedConfiguration!!.name)
    }

}

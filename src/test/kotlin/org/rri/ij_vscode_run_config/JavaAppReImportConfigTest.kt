package org.rri.ij_vscode_run_config

import com.intellij.execution.RunManager
import org.intellij.lang.annotations.Language

class JavaAppReImportConfigTest : BaseImportTestCase() {

    @Language("JSON")
    private val oldLaunchFileContent: String = """
        {
            "version": "0.2.0",
            "configurations": [
                {
                    "type": "java",
                    "name": "ReImport",
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
          <configuration name="ReImport" type="Application" factoryName="Application">
            <option name="MAIN_CLASS_NAME" value="example.Main" />
            <module name="VSCode_Import_Run_Config_Test" />
            <option name="WORKING_DIRECTORY" value="${'$'}ProjectFileDir${'$'}" />
            <method v="2">
              <option name="Make" enabled="true" />
            </method>
          </configuration>
        </component>
        """.trimIndent()

    @Language("JSON")
    private val newLaunchFileContent: String = """
        {
            "version": "0.2.0",
            "configurations": [
                {
                    "type": "java",
                    "name": "ReImport Rename",
                    "request": "launch",
                    
                    "mainClass": "example.Main",
                    "cwd": "${'$'}{workspaceFolder}"
                }
            ]
        }
        """.trimIndent()

    fun testJavaReImportNoRename() {
        setFileText(myLaunchFile, oldLaunchFileContent)
        val runManager = RunManager.getInstance(project)

        assertEquals(0, runManager.allSettings.size)

        val importConfigManager = ImportConfigManager(project, myContext)
        importConfigManager.process()
        importConfigManager.process()

        assertEquals(1, runManager.allSettings.size)
        assertSameFileWithText(xmlOutput, getOutPath().resolve("ReImport.xml"))
    }

    fun testJavaReImportRename() {
        setFileText(myLaunchFile, oldLaunchFileContent)
        val runManager = RunManager.getInstance(project)

        assertEquals(0, runManager.allSettings.size)

        val importConfigManager = ImportConfigManager(project, myContext)
        importConfigManager.process()
        setFileText(myLaunchFile, newLaunchFileContent)
        importConfigManager.process()

        assertEquals(1, runManager.allSettings.size)
        assertSameFileWithText(xmlOutput, getOutPath().resolve("ReImport.xml"))
    }

}
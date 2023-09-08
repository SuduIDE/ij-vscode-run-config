package org.rri.ij_vscode_run_config

import com.intellij.execution.RunManager
import org.intellij.lang.annotations.Language
import org.rri.ij_vscode_run_config.logic.ImportManager

class JavaAppReImportConfigTest : BaseImportTestCase() {

    fun testJavaReImportNoRename() {
        setFileText(myLaunchFile, launchJsonContentOld)
        val runManager = RunManager.getInstance(project)

        assertEquals(0, runManager.allSettings.size)

        val importManager = ImportManager(project, myContext)
        importManager.deserialize()
        importManager.importAllConfigurations()
        importManager.importAllConfigurations()

        assertEquals(1, runManager.allSettings.size)
        assertSameFileWithText(xmlOutput, getOutPath().resolve("ReImport$configFileNameSuffix.xml"))
    }

    fun testJavaReImportRename() {
        setFileText(myLaunchFile, launchJsonContentOld)
        val runManager = RunManager.getInstance(project)

        assertEquals(0, runManager.allSettings.size)

        val importManager = ImportManager(project, myContext)
        importManager.deserialize()
        importManager.importAllConfigurations()
        setFileText(myLaunchFile, launchJsonContentNew)
        importManager.deserialize()
        importManager.importAllConfigurations()

        assertEquals(1, runManager.allSettings.size)
        assertSameFileWithText(xmlOutput, getOutPath().resolve("ReImport$configFileNameSuffix.xml"))
    }

    @Language("JSON")
    private val launchJsonContentOld: String = """
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
          <configuration name="ReImport$configNameSuffix" type="Application" factoryName="Application">
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
    private val launchJsonContentNew: String = """
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

}

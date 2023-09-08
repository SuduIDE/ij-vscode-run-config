package org.rri.ij_vscode_run_config

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.IdeaTestUtil
import com.intellij.util.SystemProperties
import org.intellij.lang.annotations.Language
import org.rri.ij_vscode_run_config.logic.ImportManager

class JavaSettingsJsonTest : BaseImportTestCase() {

    fun testJavaSettingsJsonConfig() {
        setFileText(myLaunchFile, launchJsonContent)

        val settingsFile: VirtualFile = createChildData(myVSCodeFolder, "settings.json")
        setFileText(settingsFile, getSettingsJsonContent())
        assertSameFileWithText(getSettingsJsonContent(), settingsFile.toNioPath())

        val importManager = ImportManager(project, myContext)
        importManager.deserialize()
        importManager.importAllConfigurations()

        assertSameFileWithText(xmlOutput, getOutPath().resolve("Settings$configFileNameSuffix.xml"))
    }

    @Language("JSON")
    private val launchJsonContent: String = """
        {
            "version": "0.2.0",
            "configurations": [
                {
                    "type": "java",
                    "name": "Settings",
                    "request": "launch",
                    "mainClass": "example.Main"
                }
            ]
        }
        """.trimIndent()

    @Language("XML")
    private val xmlOutput: String = """
        <component name="ProjectRunConfigurationManager">
          <configuration name="Settings$configNameSuffix" type="Application" factoryName="Application">
            <option name="ALTERNATIVE_JRE_PATH" value="${SystemProperties.getJavaHome()}" />
            <option name="ALTERNATIVE_JRE_PATH_ENABLED" value="true" />
            <option name="MAIN_CLASS_NAME" value="example.Main" />
            <module name="VSCode_Import_Run_Config_Test" />
            <option name="VM_PARAMETERS" value="-DSettings=JSON" />
            <method v="2">
              <option name="Make" enabled="true" />
            </method>
          </configuration>
        </component>
        """.trimIndent()

    private fun getSettingsJsonContent(): String {
        @Language("JSON")
        val settingsFileContent: String = """
        {
            "java.debug.settings.vmArgs": "-DSettings=JSON",
            "java.configuration.runtimes": [ 
                {
                    "name": "JavaSE-9",
                    "path": "$javaHome",
                    "default": true
                },
                {
                    "name": "JavaSE-11",
                    "path": "${IdeaTestUtil.getMockJdk11().homePath}",
                    "default": false
                }
            ]
        }
        """.trimIndent()

        return settingsFileContent
    }

}

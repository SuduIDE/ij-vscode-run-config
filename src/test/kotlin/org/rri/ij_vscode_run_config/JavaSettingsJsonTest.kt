package org.rri.ij_vscode_run_config

import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.IdeaTestUtil
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.util.SystemProperties
import org.intellij.lang.annotations.Language

class JavaSettingsJsonTest : BaseImportTestCase() {

    private val javaExec =
        if (SystemInfo.isWindows) PlatformTestUtil.getJavaExe().replace("\\", "\\\\") else PlatformTestUtil.getJavaExe()
    private val javaHome = if (SystemInfo.isWindows) SystemProperties.getJavaHome()
        .replace("\\", "\\\\") else SystemProperties.getJavaHome()


    @Language("JSON")
    private val launchFileContent: String = """
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
          <configuration name="Settings" type="Application" factoryName="Application">
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

    fun testJavaSettingsJsonConfig() {
        setFileText(myLaunchFile, launchFileContent)

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

        val settingsFile: VirtualFile = createChildData(myVSCodeFolder, "settings.json")
        setFileText(settingsFile, settingsFileContent)
        assertSameFileWithText(settingsFileContent, settingsFile.toNioPath())

        val importConfigManager = ImportConfigManager(project, myContext)
        importConfigManager.process()

        assertSameFileWithText(xmlOutput, getOutPath().resolve("Settings.xml"))
    }

}
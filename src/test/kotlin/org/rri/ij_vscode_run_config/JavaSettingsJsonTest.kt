package org.rri.ij_vscode_run_config

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.IdeaTestUtil
import org.intellij.lang.annotations.Language

class JavaSettingsJsonTest : BaseImportTestCase() {

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
            <option name="ALTERNATIVE_JRE_PATH" value="${IdeaTestUtil.requireRealJdkHome()}" />
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

    override fun setUp() {
        super.setUp()

        @Language("JSON")
        val settingsFileContent: String = """
        {
            "java.debug.settings.vmArgs": "-DSettings=JSON",
            "java.configuration.runtimes": [ 
                {
                    "name": "JavaSE-9",
                    "path": "${IdeaTestUtil.requireRealJdkHome()}",
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
    }

    fun testJavaSettingsJsonConfig() {
        println("JAVAHOME: " + IdeaTestUtil.requireRealJdkHome())

        setFileText(myLaunchFile, launchFileContent)

        val importConfigManager = ImportConfigManager(project, myContext)
        importConfigManager.process()

        assertSameFileWithText(xmlOutput, getOutPath().resolve("Settings.xml"))
    }

}
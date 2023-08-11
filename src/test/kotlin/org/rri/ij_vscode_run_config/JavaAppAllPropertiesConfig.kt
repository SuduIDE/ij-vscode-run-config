package org.rri.ij_vscode_run_config

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.util.SystemProperties
import org.intellij.lang.annotations.Language
import java.io.File
import java.nio.file.Paths

class JavaAppAllPropertiesConfig: BaseImportTestCase() {

    @Language("JSON")
    private val launchFileContent: String = """
        {
            "version": "0.2.0",
            "configurations": [
                {
                    "type": "java",
                    "name": "All properties",
                    "request": "launch",
                    
                    "args": ["Hello,", "world!"],
                    "env": {
                        "TEST_VAR_FOO": "foo",
                        "TEST_VAR_BAR": "bar"
                    },
                    "envFile": "${'$'}{fileWorkspaceFolder}/.env",
                    "cwd": "${'$'}{workspaceFolder}",

                    "mainClass": "example.Main",
                    "classPaths": ["${'$'}Auto","/append/class/path/bar", "!/exclude/class/path/foo"],
                    "modulePaths": ["!/exclude/module/path/boo1", "!/exclude/module/path/boo2"],
                    "encoding": "UTF-8",
                    "vmArgs": ["-Xms100m", "-Xmx1000m"],
                    "shortenCommandLine": "jarmanifest",
                    "javaExec": "${PlatformTestUtil.getJavaExe()}",

                    "preLaunchTask": {
                        "task": "Task name",
                        "type": "shell"
                    }
                }
            ]
        }
        """.trimIndent()

    @Language("XML")
    private val xmlOutput: String = """
        <component name="ProjectRunConfigurationManager">
          <configuration name="All properties" type="Application" factoryName="Application">
            <option name="ALTERNATIVE_JRE_PATH" value="${SystemProperties.getJavaHome()}" />
            <option name="ALTERNATIVE_JRE_PATH_ENABLED" value="true" />
            <classpathModifications>
              <entry exclude="true" path="/exclude/module/path/boo1" />
              <entry exclude="true" path="/exclude/module/path/boo2" />
              <entry path="/append/class/path/bar" />
              <entry exclude="true" path="/exclude/class/path/foo" />
            </classpathModifications>
            <envs>
              <env name="TEST_VAR_FOO" value="foo" />
              <env name="TEST_VAR_BAR" value="bar" />
              <env name="IMPORT" value="TOOL" />
              <env name="TWO_PLUS_TWO" value="FOUR" />
            </envs>
            <option name="MAIN_CLASS_NAME" value="example.Main" />
            <module name="VSCode_Import_Run_Config_Test" />
            <option name="PROGRAM_PARAMETERS" value="Hello, world!" />
            <shortenClasspath name="MANIFEST" />
            <option name="VM_PARAMETERS" value="-Xms100m -Xmx1000m -Dfile.encoding=UTF-8" />
            <option name="WORKING_DIRECTORY" value="${'$'}ProjectFileDir${'$'}" />
            <method v="2">
              <option name="Make" enabled="true" />
            </method>
          </configuration>
        </component>
        """.trimIndent()

    private val envFileContent: String = """
        TWO_PLUS_TWO=FOUR
        IMPORT=TOOL
    """.trimIndent()

    fun testJavaAppAllPropertiesConfig() {
        println("JAVAHOME1: " + SystemProperties.getJavaHome())
        println("JAVAHOME2: " + SystemProperties.getJavaHome().replace('/', File.separatorChar))

        setFileText(myLaunchFile, launchFileContent)

        val envFile: VirtualFile = createChildData(myRoot, ".env")
        setFileText(envFile, envFileContent)

        val importConfigManager = ImportConfigManager(project, myContext)
        importConfigManager.process()

        assertSameFileWithText(xmlOutput, getOutPath().resolve("All_properties.xml"))
    }

}
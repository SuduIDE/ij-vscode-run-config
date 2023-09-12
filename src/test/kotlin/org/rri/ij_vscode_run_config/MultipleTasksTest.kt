package org.rri.ij_vscode_run_config

import com.intellij.execution.RunManager
import org.intellij.lang.annotations.Language
import org.rri.ij_vscode_run_config.logic.ImportManager
import java.io.File

class MultipleTasksTest : BaseImportTestCase() {

    fun testMultipleTasks() {
        val runManager = RunManager.getInstance(project)

        setFileText(myTasksFile, getTasksJsonContent())

        assertEquals(0, runManager.allSettings.size)

        val importManager = ImportManager(project, myContext)
        importManager.deserialize()
        importManager.importAllConfigurations()

        assertEquals(3, runManager.allSettings.size)

        assertSameFileWithText(getMavenXmlContent(), getOutPath().resolve("Maven_verify$configFileNameSuffix.xml"), false)
        assertSameFileWithText(getShellXmlContent(), getOutPath().resolve("Shell_text_script$configFileNameSuffix.xml"), false)
        assertSameFileWithText(getGradleXmlContent(), getOutPath().resolve("Gradle_run$configFileNameSuffix.xml"), false)
    }

    private fun getTasksJsonContent() : String {
        @Language("JSON")
        val tasksContent: String = """
        {
            "version": "2.0.0",
            "tasks": [
                {
                    "label": "Maven verify",
                    "type": "shell",
                    "command": "mvn -B verify",
                    "group": "build"
                },
                {
                    "label": "Shell text script",
                    "type": "shell",
                    "command": "echo",
                    "args": ["Hello,", "world!"],
                    "options": {
                        "cwd": "${'$'}{workspaceFolder}",
                        "env": {
                            "ONE": "ODIN",
                            "TWO": "DVA",
                            "THREE": "TRI"
                        }
                    }
                },
                {
                    "label": "Gradle run",
                    "type": "gradle",
                    "id": "${'$'}{workspaceFolder}:run",
                    "script": "run",
                    "description": "Runs this project as a JVM application",
                    "group": "application",
                    "project": "${project.name}",
                    "buildFile": "${'$'}{workspaceFolder}/build.gradle.kts",
                    "rootProject": "${project.name}",
                    "projectFolder": "${'$'}{workspaceFolder}",
                    "workspaceFolder": "${'$'}{workspaceFolder}",
                    "args": "--dry-run",
                    "javaDebug": true,
                    "problemMatcher": [
                        "${'$'}gradle"
                    ],
                    "options": {
                        "cwd": "${'$'}{workspaceFolder}",
                        "env": {
                            "ONE": "ODIN",
                            "TWO": "DVA",
                            "THREE": "TRI"
                        }
                    }
                }
            ]
        }
        """.trimIndent()

        return tasksContent
    }

    private fun getMavenXmlContent(): String {
        @Language("XML")
        val xmlOutputMaven: String = """
        <component name="ProjectRunConfigurationManager">
          <configuration name="Maven verify$configNameSuffix" type="MavenRunConfiguration" factoryName="Maven">
            <MavenSettings>
              <option name="myGeneralSettings" />
              <option name="myRunnerSettings" />
              <option name="myRunnerParameters">
                <MavenRunnerParameters>
                  <option name="profiles">
                    <set />
                  </option>
                  <option name="goals">
                    <list>
                      <option value="-B" />
                      <option value="verify" />
                    </list>
                  </option>
                  <option name="pomFileName" />
                  <option name="profilesMap">
                    <map />
                  </option>
                  <option name="resolveToWorkspace" value="false" />
                  <option name="workingDirPath" value="${'$'}PROJECT_DIR${'$'}" />
                </MavenRunnerParameters>
              </option>
            </MavenSettings>
            <method v="2" />
          </configuration>
        </component>
        """.trimIndent()

        return xmlOutputMaven
    }

    private fun getShellXmlContent(): String {
        @Language("XML")
        val xmlOutputShell: String = """
        <component name="ProjectRunConfigurationManager">
          <configuration name="Shell text script$configNameSuffix" type="ShConfigurationType">
            <option name="SCRIPT_TEXT" value="echo" />
            <option name="INDEPENDENT_SCRIPT_PATH" value="true" />
            <option name="SCRIPT_PATH" value="" />
            <option name="SCRIPT_OPTIONS" value="Hello, world!" />
            <option name="INDEPENDENT_SCRIPT_WORKING_DIRECTORY" value="$pathEqualsIndependent" />
            <option name="SCRIPT_WORKING_DIRECTORY" value="${'$'}PROJECT_DIR${'$'}" />
            <option name="INDEPENDENT_INTERPRETER_PATH" value="$pathEqualsIndependent" />
            <option name="INTERPRETER_PATH" value="$interpreterPath" />
            <option name="INTERPRETER_OPTIONS" value="" />
            <option name="EXECUTE_IN_TERMINAL" value="true" />
            <option name="EXECUTE_SCRIPT_FILE" value="false" />
            <envs>
              <env name="ONE" value="ODIN" />
              <env name="TWO" value="DVA" />
              <env name="THREE" value="TRI" />
            </envs>
            <method v="2" />
          </configuration>
        </component>
        """.trimIndent()

        return xmlOutputShell
    }

    private fun getGradleXmlContent(): String {
        @Language("XML")
        val xmlOutputGradle: String = """
        <component name="ProjectRunConfigurationManager">
          <configuration name="Gradle run$configNameSuffix" type="GradleRunConfiguration" factoryName="Gradle">
            <ExternalSystemSettings>
              <option name="env">
                <map>
                  <entry key="ONE" value="ODIN" />
                  <entry key="THREE" value="TRI" />
                  <entry key="TWO" value="DVA" />
                </map>
              </option>
              <option name="executionName" />
              <option name="externalProjectPath" value="$cwd${File.separatorChar}${project.name}" />
              <option name="externalSystemIdString" value="GRADLE" />
              <option name="scriptParameters" value="--dry-run" />
              <option name="taskDescriptions">
                <list />
              </option>
              <option name="taskNames">
                <list>
                  <option value="run" />
                </list>
              </option>
              <option name="vmOptions" />
            </ExternalSystemSettings>
            <ExternalSystemDebugServerProcess>true</ExternalSystemDebugServerProcess>
            <ExternalSystemReattachDebugProcess>true</ExternalSystemReattachDebugProcess>
            <DebugAllEnabled>false</DebugAllEnabled>
            <ForceTestExec>false</ForceTestExec>
            <method v="2" />
          </configuration>
        </component>
        """.trimIndent()

        return xmlOutputGradle
    }

}

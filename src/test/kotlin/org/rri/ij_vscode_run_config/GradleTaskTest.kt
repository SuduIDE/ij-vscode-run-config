package org.rri.ij_vscode_run_config

import org.intellij.lang.annotations.Language
import org.rri.ij_vscode_run_config.logic.ImportManager
import java.io.File

class GradleTaskTest : BaseImportTestCase() {

    fun testGradleBuildTask() {
        setFileText(myTasksFile, getGradleBuildTasksContent())

        val importManager = ImportManager(project, myContext)
        importManager.deserialize()
        importManager.importAllConfigurations()

        assertSameFileWithText(getGradleBuildXmlContent(), getOutPath().resolve("Gradle_build$configFileNameSuffix.xml"), false)
    }

    fun testGradleRunTask() {
        setFileText(myTasksFile, getGradleRunTasksContent())

        val importManager = ImportManager(project, myContext)
        importManager.deserialize()
        importManager.importAllConfigurations()

        assertSameFileWithText(getGradleRunXmlContent(), getOutPath().resolve("Gradle_run$configFileNameSuffix.xml"), false)
    }

    private fun getGradleBuildTasksContent(): String {
        @Language("JSON")
        val tasksContent: String = """
            { 
                "version": "2.0.0",
                "tasks": [
                    {
                        "label": "Gradle build",
                        "type": "gradle",
                        "id": "${'$'}{workspaceFolder}:build",
                        "script": "build",
                        "description": "Assembles and tests this project.",
                        "group": "build",
                        "project": "${project.name}",
                        "buildFile": "${'$'}{workspaceFolder}/build.gradle.kts",
                        "rootProject": "${project.name}",
                        "projectFolder": "${'$'}{workspaceFolder}",
                        "workspaceFolder": "${'$'}{workspaceFolder}",
                        "args": "--dry-run",
                        "javaDebug": false,
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

    private fun getGradleBuildXmlContent(): String {
        @Language("XML")
        val xmlOutput: String = """
        <component name="ProjectRunConfigurationManager">
          <configuration name="Gradle build$configNameSuffix" type="GradleRunConfiguration" factoryName="Gradle">
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
                  <option value="build" />
                </list>
              </option>
              <option name="vmOptions" />
            </ExternalSystemSettings>
            <ExternalSystemDebugServerProcess>false</ExternalSystemDebugServerProcess>
            <ExternalSystemReattachDebugProcess>true</ExternalSystemReattachDebugProcess>
            <DebugAllEnabled>false</DebugAllEnabled>
            <ForceTestExec>false</ForceTestExec>
            <method v="2" />
          </configuration>
        </component>
        """.trimIndent()

        return xmlOutput
    }

    private fun getGradleRunTasksContent(): String {
        @Language("JSON")
        val tasksContent: String = """
            { 
                "version": "2.0.0",
                "tasks": [
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

    private fun getGradleRunXmlContent(): String {
        @Language("XML")
        val xmlOutput: String = """
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

        return xmlOutput
    }

}
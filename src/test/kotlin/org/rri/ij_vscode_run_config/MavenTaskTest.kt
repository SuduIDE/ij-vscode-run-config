package org.rri.ij_vscode_run_config

import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.SystemInfo
import org.intellij.lang.annotations.Language
import org.rri.ij_vscode_run_config.logic.ImportManager

class MavenTaskTest : BaseImportTestCase() {

    fun testMavenTask() {
        setFileText(myTasksFile, getTasksJsonContent())

        val importManager = ImportManager(project, myContext)
        importManager.deserialize()
        importManager.importAllConfigurations()

        assertSameFileWithText(getXmlOutput(), getOutPath().resolve("Maven_compile$configFileNameSuffix.xml"), false)
    }

    private fun getTasksJsonContent(): String {
        @Language("JSON")
        val tasksJsonContent: String = """
        {
            "version": "2.0.0",
            "tasks": [
                {
                    "label": "Maven compile",
                    "type": "shell",
                    "command": "mvn compile --debug",
                    "group": "build",
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

        return tasksJsonContent
    }

    private fun getXmlOutput(): String {
        @Language("XML")
        val xmlOutput: String = """
        <component name="ProjectRunConfigurationManager">
          <configuration name="Maven compile$configNameSuffix" type="MavenRunConfiguration" factoryName="Maven">
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
                      <option value="compile" />
                      <option value="--debug" />
                    </list>
                  </option>
                  <option name="pomFileName" />
                  <option name="profilesMap">
                    <map />
                  </option>
                  <option name="resolveToWorkspace" value="false" />
                  <option name="workingDirPath" value="$cwd" />
                </MavenRunnerParameters>
              </option>
            </MavenSettings>
            <method v="2" />
          </configuration>
        </component>
        """.trimIndent()

        return xmlOutput
    }



}

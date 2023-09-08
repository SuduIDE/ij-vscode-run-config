package org.rri.ij_vscode_run_config

import org.intellij.lang.annotations.Language
import org.rri.ij_vscode_run_config.logic.ImportManager

class JavaRemoteConfigTest: BaseImportTestCase() {

    fun testJavaRemoteConfigDefault() {
        setFileText(myLaunchFile, launchJsonContentDefault)

        val importManager = ImportManager(project, myContext)
        importManager.deserialize()
        importManager.importAllConfigurations()

        assertSameFileWithText(xmlOutputDefault, getOutPath().resolve("Remote_Default$configFileNameSuffix.xml"))
    }

    fun testJavaRemoteConfig() {
        setFileText(myLaunchFile, launchJsonContent)

        val importManager = ImportManager(project, myContext)
        importManager.deserialize()
        importManager.importAllConfigurations()

        assertSameFileWithText(xmlOutput, getOutPath().resolve("Remote$configFileNameSuffix.xml"))
    }

    @Language("JSON")
    private val launchJsonContentDefault: String = """
        {
            "version": "0.2.0",
            "configurations": [
                {
                    "type": "java",
                    "name": "Remote Default",
                    "request": "attach",
                    
                    "hostName": "localhost",
                    "port": 5005
                }
            ]
        }
        """.trimIndent()

    @Language("XML")
    private val xmlOutputDefault: String = """
        <component name="ProjectRunConfigurationManager">
          <configuration name="Remote Default$configNameSuffix" type="Remote">
            <option name="USE_SOCKET_TRANSPORT" value="true" />
            <option name="SERVER_MODE" value="false" />
            <option name="SHMEM_ADDRESS" value="javadebug" />
            <option name="HOST" value="localhost" />
            <option name="PORT" value="5005" />
            <option name="AUTO_RESTART" value="false" />
            <method v="2" />
          </configuration>
        </component>
        """.trimIndent()

    @Language("JSON")
    private val launchJsonContent: String = """
        {
            "version": "0.2.0",
            "configurations": [
                {
                    "type": "java",
                    "name": "Remote",
                    "request": "attach",
                    
                    "hostName": "12.34.56.78",
                    "port": 12345
                }
            ]
        }
        """.trimIndent()

    @Language("XML")
    private val xmlOutput: String = """
        <component name="ProjectRunConfigurationManager">
          <configuration name="Remote$configNameSuffix" type="Remote">
            <option name="USE_SOCKET_TRANSPORT" value="true" />
            <option name="SERVER_MODE" value="false" />
            <option name="SHMEM_ADDRESS" value="javadebug" />
            <option name="HOST" value="12.34.56.78" />
            <option name="PORT" value="12345" />
            <option name="AUTO_RESTART" value="false" />
            <method v="2" />
          </configuration>
        </component>
        """.trimIndent()

}

package org.rri.ij_vscode_run_config

import org.intellij.lang.annotations.Language

class JavaRemoteConfigTest: BaseImportTestCase() {

    @Language("JSON")
    private val launchFileContentDefault: String = """
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
          <configuration name="Remote Default" type="Remote">
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

    fun testJavaRemoteConfigDefault() {
        setFileText(myLaunchFile, launchFileContentDefault)

        val importConfigManager = ImportConfigManager(project, myContext)
        importConfigManager.process()

        assertSameFileWithText(xmlOutputDefault, getOutPath().resolve("Remote_Default.xml"))
    }

    @Language("JSON")
    private val launchFileContent: String = """
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
          <configuration name="Remote" type="Remote">
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

    fun testJavaRemoteConfig() {
        setFileText(myLaunchFile, launchFileContent)

        val importConfigManager = ImportConfigManager(project, myContext)
        importConfigManager.process()

        assertSameFileWithText(xmlOutput, getOutPath().resolve("Remote.xml"))
    }

}
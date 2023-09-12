package org.rri.ij_vscode_run_config

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.PathMacroManager
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.HeavyTestHelper
import com.intellij.testFramework.JavaProjectTestCase
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.util.SystemProperties
import com.intellij.util.io.systemIndependentPath
import kotlinx.coroutines.runBlocking
import org.intellij.lang.annotations.Language
import org.jetbrains.concurrency.await
import org.rri.ij_vscode_run_config.logic.CONFIGURATION_NO_DEPENDENCIES_SUFFIX
import org.rri.ij_vscode_run_config.logic.PLUGIN_CONFIGURATION_NAME_SUFFIX
import java.io.IOException
import java.nio.file.Path

abstract class BaseImportTestCase : JavaProjectTestCase() {

    protected lateinit var myContext: DataContext
    protected lateinit var myRoot: VirtualFile
    protected lateinit var myVSCodeFolder: VirtualFile
    protected lateinit var myLaunchFile: VirtualFile
    protected lateinit var myTasksFile: VirtualFile

    protected val configNameSuffix: String = PLUGIN_CONFIGURATION_NAME_SUFFIX
    protected val configFileNameSuffix: String = FileUtil.sanitizeFileName(configNameSuffix, true)
    protected val noDepsSuffix: String = CONFIGURATION_NO_DEPENDENCIES_SUFFIX
    protected val noDepsFileSuffix: String = FileUtil.sanitizeFileName(noDepsSuffix, true)

    // For WINDOWS JSONs created via raw JAVA String
    protected val javaExec: String = if (SystemInfo.isWindows) PlatformTestUtil.getJavaExe().replace("\\", "\\\\") else PlatformTestUtil.getJavaExe()
    protected val javaHome: String = if (SystemInfo.isWindows) SystemProperties.getJavaHome().replace("\\", "\\\\") else SystemProperties.getJavaHome()

    protected val defaultShell: String = if (SystemInfo.isWindows) "pws" else "bash"
    protected var pathEqualsIndependent: Boolean = true
    protected lateinit var interpreterPath: String
    protected lateinit var cwd: String

    @Language("Java")
    private val mainContent: String = """
        package example; 
        public class Main {
            public static void main(java.lang.String[] args) {
                System.out.println("Hello World!");
            }
        }
        """.trimIndent()

    protected fun getOutPath(): Path = myRoot.toNioPath().resolve(".idea/runConfigurations")

    final override fun isCreateDirectoryBasedProject() = true

    override fun setUpModule() {
        try {
            WriteCommandAction.writeCommandAction(project).run<IOException> {
                myModule = createModule("VSCode_Import_Run_Config_Test")
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    override fun setUpProject() {
        super.setUpProject()

        ApplicationManagerEx.getApplicationEx().isSaveAllowed = true

        runBlocking {
            myContext = DataManager.getInstance().dataContextFromFocusAsync.await()
        }

        val baseDir: VirtualFile = getOrCreateProjectBaseDir()
        myRoot = HeavyTestHelper.createTestProjectStructure(myModule, baseDir.path, baseDir.toNioPath(), true)

        createMainFile(myRoot)

        myVSCodeFolder = createChildDirectory(myRoot, ".vscode")
        myLaunchFile = createChildData(myVSCodeFolder, "launch.json")
        myTasksFile = createChildData(myVSCodeFolder, "tasks.json")

        cwd = "${'$'}PROJECT_DIR${'$'}"
        interpreterPath = detectShellPaths().stream().filter { shell ->
            shell.endsWith(defaultShell) || (SystemInfo.isWindows && defaultShell == "pws" && shell.contains("powershell"))
        }.findFirst().orElse("")

        if (SystemInfo.isWindows) {
            val newPath: String = project.guessProjectDir()!!.toNioPath().relativize(Path.of(interpreterPath)).systemIndependentPath
            interpreterPath = "${'$'}PROJECT_DIR${'$'}" + '/' + newPath.removePrefix(project.guessProjectDir()!!.path)
            pathEqualsIndependent = false
            cwd = project.guessProjectDir()!!.toNioPath().toString()
        }
    }

    private fun createMainFile(root: VirtualFile) {
        val srcFolder = createChildDirectory(root, "src")
        val srcExampleFolder = createChildDirectory(srcFolder, "example")
        val mainFile = createChildData(srcExampleFolder, "Main.java")

        setFileText(mainFile, mainContent)
    }

    protected fun assertSameFileWithText(expected: String, filePath: Path, expand: Boolean = true) {
        project.save()

        val actual: String = if (expand)
            PathMacroManager.getInstance(project).expandPath(PlatformTestUtil.loadFileText(filePath.toString()))
        else
            PlatformTestUtil.loadFileText(filePath.toString())

        assertEquals(expected, actual)
    }

}

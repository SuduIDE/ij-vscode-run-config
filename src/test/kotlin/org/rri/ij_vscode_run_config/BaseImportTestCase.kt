package org.rri.ij_vscode_run_config

import com.intellij.configurationStore.saveSettings
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.PathMacroManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.HeavyTestHelper
import com.intellij.testFramework.JavaProjectTestCase
import kotlinx.coroutines.runBlocking
import org.intellij.lang.annotations.Language
import org.jetbrains.concurrency.await
import java.io.IOException
import java.nio.file.Path

abstract class BaseImportTestCase : JavaProjectTestCase() {

    protected lateinit var myContext: DataContext
    protected lateinit var myRoot: VirtualFile
    protected lateinit var myVSCodeFolder: VirtualFile
    protected lateinit var myLaunchFile: VirtualFile

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

        runBlocking {
            saveSettings(project)
            myContext = DataManager.getInstance().dataContextFromFocusAsync.await()
        }

        val baseDir: VirtualFile = getOrCreateProjectBaseDir()
        myRoot = HeavyTestHelper.createTestProjectStructure(myModule, baseDir.path, baseDir.toNioPath(), true)

        createMainFile(myRoot)

        myVSCodeFolder = createChildDirectory(myRoot, ".vscode")
        myLaunchFile = createChildData(myVSCodeFolder, "launch.json")
    }

    private fun createMainFile(root: VirtualFile) {
        val srcFolder = createChildDirectory(root, "src")
        val srcExampleFolder = createChildDirectory(srcFolder, "example")
        val mainFile = createChildData(srcExampleFolder, "Main.java")

        setFileText(mainFile, mainContent)
    }

    protected fun assertSameFileWithText(expected: String, filePath: Path) {
        runBlocking {
            saveSettings(project)
        }

        val vFile: VirtualFile = LocalFileSystem.getInstance().findFileByIoFile(filePath.toFile())!!
        val actual: String =
            PathMacroManager.getInstance(project).expandPath(String(vFile.contentsToByteArray(), vFile.charset))
        assertEquals(expected, actual)
    }

}
package org.rri.ij_vscode_run_config

import com.intellij.ide.macro.*
import com.intellij.openapi.actionSystem.DataContext
import java.io.File
import java.nio.file.Paths
import java.util.regex.Matcher
import java.util.regex.Pattern

class VariableRepository private constructor() {
    private val pattern: Pattern = Pattern.compile("\\$\\{(([a-zA-Z]|[0-9])+)}")
    //    private val unsupported: Pattern = Pattern.compile("\\$\\{(([a-zA-Z]|[0-9])+)}")

    companion object {
        @Volatile
        private var instance: VariableRepository? = null

        fun getInstance(): VariableRepository {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = VariableRepository()
                    }
                }
            }
            return instance!!
        }
    }

    fun contains(str: String): Boolean {
        return pattern.matcher(str).find()
    }

    fun substituteAllVariables(str: String): String {
        val matcher: Matcher = pattern.matcher(str)
        var result: String = str

        while (matcher.find()) {
            val oldValue: String = matcher.group(0)
            val newValue: String = substituteSingleVariable(oldValue)
            result = str.replace(oldValue, substituteSingleVariable(newValue))
        }

        return result
    }

    private fun substituteSingleVariable(variable: String): String {
        return when (variable) {
            "\${userHome}" -> System.getProperty("user.home")
            "\${workspaceFolder}" -> "\$" + ProjectFileDirMacro().name + "\$"
            "\${workspaceFolderBasename}" -> "\$" + Paths.get(ProjectFileDirMacro().name).fileName.toString() + "\$"
            "\${file}" -> "\$" + FilePathMacro().name + "\$"
            "\${fileWorkspaceFolder}" -> "\$" + ProjectFileDirMacro().name + "\$"
            "\${relativeFile}" -> "\$" + FilePathRelativeToProjectRootMacro().name + "\$"
            "\${relativeFileDirname}" -> "\$" + FileDirRelativeToProjectRootMacro().name + "\$"
            "\${fileBasename}" -> "\$" + FileNameMacro().name + "\$"
            "\${fileBasenameNoExtension}" -> "\$" + FileNameWithoutAllExtensions().name + "\$"
            "\${fileExtname}" -> "\$" + FileExtMacro().name + "\$"
            "\${fileDirname}" -> "\$" + FileDirMacro().name + "\$"
            "\${fileDirnameBasename}" -> "\$" + FileDirNameMacro().name + "\$"
            "\${cwd}" -> "\$" + ProjectFileDirMacro().name + "\$"
            "\${lineNumber}" -> "\$" + LineNumberMacro().name + "\$"
            "\${selectedText}" -> "\$" + SelectedTextMacro().name + "\$"
            "\${pathSeparator}" -> File.separatorChar.toString()
            "\${execPath}" -> throw ImportException("Forbidden predefined variable: \${execPath}")
            "\${defaultBuildTask}" -> throw ImportException("Forbidden predefined variable: \${defaultBuildTask}")
            else -> variable
        }
    }

    fun expandMacrosInString(str: String, context: DataContext): String {
        val strWithSubstitutions = substituteAllVariables(str)
        return MacroManager.getInstance().expandMacrosInString(strWithSubstitutions, false, context)
            ?: throw ImportException("Cannot execute macro expansion in string: $str")
    }

}
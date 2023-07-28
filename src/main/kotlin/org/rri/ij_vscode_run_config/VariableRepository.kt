package org.rri.ij_vscode_run_config

import com.intellij.ide.macro.*
import com.intellij.openapi.actionSystem.DataContext
import java.io.File
import java.nio.file.Paths
import java.util.regex.Matcher
import java.util.regex.Pattern

class VariableRepository private constructor() {
    private val pattern: Pattern = Pattern.compile("\\$\\{(([a-zA-Z]|[0-9])+)}")
    private val unsupportedPattern: Pattern = Pattern.compile("\\$\\{((input:)|(command:)|(config:))([a-zA-Z]|[0-9]|[^\\w\\s])*}")
    private val envPattern: Pattern = Pattern.compile("\\$\\{(env:)([a-zA-Z]|[0-9]|[^\\w\\s])*}")

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
        val unsupportedMatcher = unsupportedPattern.matcher(str)
        if (unsupportedMatcher.find()) {
            throw ImportException("String contains unsupported variables: ${unsupportedMatcher.group()}")
        }

        var result: String = str

        val matcher: Matcher = pattern.matcher(str)
        while (matcher.find()) {
            val oldValue: String = matcher.group()
            val newValue: String = substitutePredefinedVariable(oldValue)
            result = str.replace(oldValue, newValue)
        }

        val envMatcher = envPattern.matcher(result)
        while (envMatcher.find()) {
            val oldValue: String = envMatcher.group()
            val newValue: String = substituteEnvVariable(oldValue)
            result = result.replace(oldValue, newValue)
        }

        return result
    }

    private fun substitutePredefinedVariable(str: String): String {
        return when (str) {
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
            else -> str
        }
    }

    private fun substituteEnvVariable(str: String): String {
        val variable: String = str.substring("\${env:".length, str.length - "}".length)
        return System.getenv(variable) ?: throw ImportException("Cannot find specified environment variable: $variable")
    }

    fun expandMacrosInString(str: String, context: DataContext): String {
        val strWithSubstitutions = substituteAllVariables(str)
        return MacroManager.getInstance().expandMacrosInString(strWithSubstitutions, false, context)
            ?: throw ImportException("Cannot execute macro expansion in string: $str")
    }

}
package org.rri.ij_vscode_run_config

import com.intellij.ide.macro.*
import com.intellij.openapi.actionSystem.DataContext
import java.io.File
import java.nio.file.Paths
import java.util.regex.Matcher
import java.util.regex.Pattern

object VariableRepository {
    private val pattern: Pattern = Pattern.compile("\\$\\{(([a-zA-Z]|[0-9])+)}")
    private val unsupportedPattern: Pattern = Pattern.compile("\\$\\{((input:)|(command:)|(config:))([a-zA-Z]|[0-9]|[^\\w\\s])*}")
    private val envPattern: Pattern = Pattern.compile("\\$\\{(env:)([a-zA-Z]|[0-9]|[^\\w\\s])*}")

    private val varMap: MutableMap<String, String> = HashMap()

    private fun dollarize(str: String) = "\$" + str + "\$"

    init {
        varMap["\${userHome}"] = System.getProperty("user.home")
        varMap["\${pathSeparator}"] = File.separatorChar.toString()

        varMap["\${cwd}"] = dollarize(ProjectFileDirMacro().name)
        varMap["\${file}"] = dollarize(FilePathMacro().name)
        varMap["\${lineNumber}"] = dollarize(LineNumberMacro().name)
        varMap["\${fileExtname}"] = dollarize(FileExtMacro().name)
        varMap["\${fileDirname}"] = dollarize(FileDirMacro().name)
        varMap["\${fileBasename}"] = dollarize(FileNameMacro().name)
        varMap["\${relativeFile}"] = dollarize(FilePathRelativeToProjectRootMacro().name)
        varMap["\${selectedText}"] = dollarize(SelectedTextMacro().name)
        varMap["\${workspaceFolder}"] = dollarize(ProjectFileDirMacro().name)
        varMap["\${relativeFileDirname}"] = dollarize(FileDirRelativeToProjectRootMacro().name)
        varMap["\${fileWorkspaceFolder}"] = dollarize(ProjectFileDirMacro().name)
        varMap["\${fileDirnameBasename}"] = dollarize(FileDirNameMacro().name)
        varMap["\${fileBasenameNoExtension}"] = dollarize(FileNameWithoutAllExtensions().name)
        varMap["\${workspaceFolderBasename}"] = dollarize(Paths.get(ProjectFileDirMacro().name).fileName.toString())
    }

    @JvmStatic
    fun contains(str: String): Boolean {
        return pattern.matcher(str).find() || envPattern.matcher(str).find()
    }

    @JvmStatic
    fun substituteAllVariables(str: String): String {
        val unsupportedMatcher = unsupportedPattern.matcher(str)
        if (unsupportedMatcher.find()) {
            throw ImportError("String contains unsupported variables: ${unsupportedMatcher.group()}")
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
        if (str == "\${execPath}" || str == "\${defaultBuildTask}")
            throw ImportError("Unsupported predefined variable: $str")
        return varMap[str] ?: str
    }

    private fun substituteEnvVariable(str: String): String {
        val variable: String = str.substring("\${env:".length, str.length - "}".length)
        return System.getenv(variable) ?: ""
    }

    @JvmStatic
    fun expandMacrosInString(str: String, context: DataContext): String {
        val strWithSubstitutions = substituteAllVariables(str)
        return MacroManager.getInstance().expandMacrosInString(strWithSubstitutions, false, context)
            ?: throw ImportError("Cannot execute macro expansion in string: $str")
    }

}
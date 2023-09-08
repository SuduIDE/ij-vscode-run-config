package org.rri.ij_vscode_run_config.logic

import com.intellij.ide.macro.*
import com.intellij.openapi.actionSystem.DataContext
import java.io.File
import java.nio.file.Paths
import java.util.regex.Matcher
import java.util.regex.Pattern

object VariableRepository {
    private val pattern: Pattern = Pattern.compile("\\$\\{(([a-zA-Z]|[0-9])+)}")

    private val unsupportedPattern: Pattern =
        Pattern.compile("\\$\\{((input:)|(command:)|(config:))([a-zA-Z]|[0-9]|[^\\w\\s])*}")

    private val envPattern: Pattern = Pattern.compile("\\$\\{(env:)([a-zA-Z]|[0-9]|[^\\w\\s])*}")

    private val varMap: MutableMap<String, String> = HashMap()

    private fun dollarQuote(str: String) = "\$" + str + "\$"

    init {
        varMap["\${userHome}"] = System.getProperty("user.home")
        varMap["\${pathSeparator}"] = File.separatorChar.toString()

        varMap["\${cwd}"] = dollarQuote(ProjectFileDirMacro().name)
        varMap["\${file}"] = dollarQuote(FilePathMacro().name)
        varMap["\${lineNumber}"] = dollarQuote(LineNumberMacro().name)
        varMap["\${fileExtname}"] = dollarQuote(FileExtMacro().name)
        varMap["\${fileDirname}"] = dollarQuote(FileDirMacro().name)
        varMap["\${fileBasename}"] = dollarQuote(FileNameMacro().name)
        varMap["\${relativeFile}"] = dollarQuote(FilePathRelativeToProjectRootMacro().name)
        varMap["\${selectedText}"] = dollarQuote(SelectedTextMacro().name)
        varMap["\${workspaceFolder}"] = dollarQuote(ProjectFileDirMacro().name)
        varMap["\${relativeFileDirname}"] = dollarQuote(FileDirRelativeToProjectRootMacro().name)
        varMap["\${fileWorkspaceFolder}"] = dollarQuote(ProjectFileDirMacro().name)
        varMap["\${fileDirnameBasename}"] = dollarQuote(FileDirNameMacro().name)
        varMap["\${fileBasenameNoExtension}"] = dollarQuote(FileNameWithoutAllExtensions().name)
        varMap["\${workspaceFolderBasename}"] = dollarQuote(Paths.get(ProjectFileDirMacro().name).fileName.toString())
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

    @JvmStatic
    fun expandMacrosInString(str: String, context: DataContext): String {
        val strWithSubstitutions = substituteAllVariables(str)
        return MacroManager.getInstance().expandMacrosInString(strWithSubstitutions, false, context)
            ?: throw ImportError("Cannot execute macro expansion in string: $str")
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

}
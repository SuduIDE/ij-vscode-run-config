package org.rri.ij_vscode_run_config.builders


import com.intellij.execution.ShortenCommandLine
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.application.ApplicationConfigurationType
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ModuleBasedConfigurationOptions
import com.intellij.execution.configurations.RuntimeConfigurationWarning
import com.intellij.execution.configurations.runConfigurationType
import com.intellij.execution.util.JavaParametersUtil
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiMethodUtil
import io.github.cdimascio.dotenv.Dotenv
import io.github.cdimascio.dotenv.DotenvException
import io.github.cdimascio.dotenv.dotenv
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.rri.ij_vscode_run_config.ImportError
import org.rri.ij_vscode_run_config.ImportWarning
import org.rri.ij_vscode_run_config.VariableRepository
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.stream.Collectors
import kotlin.io.path.absolutePathString


class JavaAppConfigBuilder(name: String, project: Project): ConfigBuilderBase(name, project) {

    override val factory: ConfigurationFactory =
        runConfigurationType<ApplicationConfigurationType>().configurationFactories[0]

    override val config: ApplicationConfiguration = factory.createConfiguration(
        name,
        factory.createTemplateConfiguration(project)
    ) as ApplicationConfiguration

    fun setMainClass(value: JsonElement?): JavaAppConfigBuilder {
        println("BEFORE MAINCLASS: $name")
        val mainClassStr = value?.jsonPrimitive?.content ?: throw ImportError("Main class is not specified")

        if (VariableRepository.contains(mainClassStr)) {
            throw ImportError("VSCode config \"mainClass\" property contains variables: $mainClassStr")
        }

        val mainClassPath = Paths.get(mainClassStr)
        val mainClass: PsiClass = if (mainClassPath.isAbsolute) {
            findMainClassFromPath(mainClassPath)
        } else if (mainClassStr.count { c -> c == '/' } == 1 && !mainClassStr.startsWith("/")) {
            config.configurationModule.findClass(mainClassStr.substringAfter('/'))
        } else {
            config.configurationModule.findClass(mainClassStr)
        } ?: throw ImportError("Cannot find specified main class: $mainClassStr")

        config.setMainClass(mainClass)
        println("AFTER MAINCLASS: $name")
        return this
    }

    private fun findMainClassFromPath(path: Path): PsiClass {
        val files = FilenameIndex.getVirtualFilesByName(
            path.fileName.toString(),
            GlobalSearchScope.allScope(project)
        )
        val file: VirtualFile = if (!files.isEmpty()) {
            files.first()
        } else {
            LocalFileSystem.getInstance().findFileByIoFile(File(path.absolutePathString()))
                ?: throw ImportError("Incorrect main class argument: ${path.absolutePathString()}")
        }

        val mainFile = PsiManager.getInstance(project).findFile(file) as? PsiJavaFile
            ?: throw ImportError("Specified main class is not found!")
        for (cl: PsiClass in mainFile.classes) {
            if (PsiMethodUtil.hasMainMethod(cl)) {
                return cl
            }
        }
        throw ImportError("Incorrect main class argument: ${path.absolutePathString()}")
    }

    fun setJavaExec(value: JsonElement?, context: DataContext): JavaAppConfigBuilder {
        println("BEFORE JAVAEXEC: $name")
        var javaExecStr: String? = null
        if (value.runCatching { value?.jsonArray != null }.getOrElse { false }) {
            for (jre: JsonElement in value!!.jsonArray) {
                if (jre.jsonObject["default"]?.jsonPrimitive?.content == "true") {
                    javaExecStr = jre.jsonObject["path"]?.jsonPrimitive?.content
                }
            }
        } else {
            javaExecStr = value?.jsonPrimitive?.content
        }

        if (javaExecStr == null)
            return this

        if (!trySetJavaExecStr(javaExecStr, context)) {
            throw ImportError("Specified JDK/JRE path is invalid: $javaExecStr")
        }

        println("AFTER JAVAEXEC: $name")
        return this
    }

    private fun trySetJavaExecStr(value: String, context: DataContext): Boolean {
        var javaPath: Path = Paths.get(VariableRepository.expandMacrosInString(value, context))

        while (javaPath.nameCount != 0) {
            try {
                JavaParametersUtil.checkAlternativeJRE(javaPath.toString())
                config.alternativeJrePath = javaPath.toString()
                config.isAlternativeJrePathEnabled = true
                println("JAVAEXEC: $value")
                return true
            } catch (exc: RuntimeConfigurationWarning) {
                javaPath = javaPath.parent
            }
        }
        return false
    }

    // ! Advanced Substitution
    fun setProgramArgs(value: JsonElement?): JavaAppConfigBuilder {
        println("BEFORE ARGS: $name")
        if (value.runCatching { value?.jsonPrimitive != null }.getOrElse { false }) {
            config.programParameters = VariableRepository.substituteAllVariables(value!!.jsonPrimitive.content)
        } else if (value?.jsonArray != null) {
            config.programParameters = value.jsonArray.stream().map { m ->
                VariableRepository.substituteAllVariables(m.jsonPrimitive.content)
            }.collect(Collectors.joining(" "))
        }

        println("AFTER ARGS: $name")
        return this
    }

    // ! $Auto $Runtime $Test
    // ! Advanced Substitution
    fun setModulePaths(value: JsonElement?, context: DataContext): JavaAppConfigBuilder {
        println("BEFORE MODULEPATHS: $name")
        val modulePaths = LinkedList<ModuleBasedConfigurationOptions.ClasspathModification>()

        if (value != null) {
            for (modulePathStr in value.jsonArray) {
                addClassPathModification(modulePaths, modulePathStr, false, context)
            }
        }

        config.classpathModifications.addAll(modulePaths)
        println("AFTER MODULEPATHS: $name")
        return this
    }

    // ! $Auto $Runtime $Test
    // ! Advanced Substitution
    fun setClassPaths(value: JsonElement?, context: DataContext): JavaAppConfigBuilder {
        println("BEFORE CLASSPATHS: $name")
        val classPaths = LinkedList<ModuleBasedConfigurationOptions.ClasspathModification>()

        if (value != null) {
            for (classPathStr in value.jsonArray) {
                addClassPathModification(classPaths, classPathStr, true, context)
            }
        }

        config.classpathModifications.addAll(classPaths)
        println("AFTER CLASSPATHS: $name")
        return this
    }

    private fun addClassPathModification(
        list: LinkedList<ModuleBasedConfigurationOptions.ClasspathModification>,
        path: JsonElement,
        isClassPath: Boolean,
        context: DataContext
    ) {
        val expModuleStr: String = VariableRepository.expandMacrosInString(path.jsonPrimitive.content, context)
        if (expModuleStr == "\$Auto" || expModuleStr == "\$Runtime" || expModuleStr == "\$Test") {
            return
        } else if (expModuleStr.startsWith('!')) {
            list.add(ModuleBasedConfigurationOptions.ClasspathModification(expModuleStr.substring(1), true))
        } else if (isClassPath) {
            list.add(ModuleBasedConfigurationOptions.ClasspathModification(expModuleStr, false))
        }
    }

    // ! Advanced Substitution
    fun setVMArgs(value: JsonElement?): JavaAppConfigBuilder {
        println("BEFORE VMARGS: $name")
        val vmArgsBuilder: StringBuilder = StringBuilder()

        if (value.runCatching { value?.jsonPrimitive != null }.getOrElse { false }) {
            val vmArgsStr: String = VariableRepository.substituteAllVariables(value!!.jsonPrimitive.content)
            vmArgsBuilder.append(vmArgsStr)
        } else if (value?.jsonArray != null) {
            val vmArgs: String = value.jsonArray.stream().map { m ->
                VariableRepository.substituteAllVariables(m.jsonPrimitive.content)
            }.collect(Collectors.joining(" "))
            vmArgsBuilder.append(vmArgs)
        }

        if (config.vmParameters != null) {
            config.vmParameters += " $vmArgsBuilder"
        } else {
            config.vmParameters = vmArgsBuilder.toString()
        }

        println("AFTER VMARGS: $name")
        return this
    }

    // ! Advanced Substitution
    fun setWorkingDirectory(value: JsonElement?): JavaAppConfigBuilder {
        println("BEFORE CWD: $name")
        if (value?.jsonPrimitive?.content != null) {
            config.workingDirectory = VariableRepository.substituteAllVariables(value.jsonPrimitive.content)
        } else {
            config.workingDirectory = project.guessProjectDir()?.path
        }
        println("AFTER CWD: $name")
        return this
    }

    // ! Advanced Substitution
    fun setEnv(value: JsonElement?): JavaAppConfigBuilder {
        println("BEFORE ENV: $name")
        if (value != null) {
            val envMap: MutableMap<String, String> = HashMap()
            for (entry in value.jsonObject.entries) {
                envMap[entry.key] = VariableRepository.substituteAllVariables(entry.value.jsonPrimitive.content)
            }
            config.isPassParentEnvs = true
            config.envs.plusAssign(envMap)
        }

        println("AFTER ENV: $name")
        return this
    }

    fun setEnvFromFile(value: JsonElement?, context: DataContext): JavaAppConfigBuilder {
        println("BEFORE ENVFILE: $name")
        if (value?.jsonPrimitive?.content != null) {
            val envMap: MutableMap<String, String> = HashMap()
            val envPath: Path = Paths.get(value.jsonPrimitive.content)
            try {
                val dotenv: Dotenv = dotenv {
                    directory = VariableRepository.expandMacrosInString(envPath.parent.toString(), context)
                    filename = VariableRepository.expandMacrosInString(envPath.fileName.toString(), context)
                    systemProperties = false
                }
                for (dotenvEntry in dotenv.entries(Dotenv.Filter.DECLARED_IN_ENV_FILE)) {
                    envMap[dotenvEntry.key] = dotenvEntry.value
                }
            } catch (exc: DotenvException) {
                throw ImportWarning("Env file is invalid", exc)
            }
            config.isPassParentEnvs = true
            config.envs.plusAssign(envMap)
        }

        println("AFTER ENVFILE: $name")
        return this
    }

    fun setShortenCommandLine(value: JsonElement?): JavaAppConfigBuilder {
        println("BEFORE SHORTENCOMMANDLINE: $name")
        if (value?.jsonPrimitive?.content != null) {
            config.shortenCommandLine = when (value.jsonPrimitive.content) {
                "none" -> ShortenCommandLine.NONE
                "jarmanifest" -> ShortenCommandLine.MANIFEST
                "argfile" -> ShortenCommandLine.ARGS_FILE
                else -> null
            }
        }

        println("AFTER SHORTENCOMMANDLINE: $name")
        return this
    }

    fun setEncoding(value: JsonElement?): JavaAppConfigBuilder {
        println("BEFORE ENCODING: $name")
        val encoding: String? = value?.jsonPrimitive?.content
        if (encoding != null) {
            config.vmParameters += " -Dfile.encoding=$encoding"
        }

        println("AFTER ENCODING: $name")
        return this
    }

}
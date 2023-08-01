package org.rri.ij_vscode_run_config

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.ShortenCommandLine
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.application.ApplicationConfigurationType
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ModuleBasedConfigurationOptions
import com.intellij.execution.configurations.RuntimeConfigurationWarning
import com.intellij.execution.configurations.runConfigurationType
import com.intellij.execution.util.JavaParametersUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiMethodUtil
import io.github.cdimascio.dotenv.Dotenv
import io.github.cdimascio.dotenv.DotenvException
import io.github.cdimascio.dotenv.dotenv
import kotlinx.serialization.json.*
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.stream.Collectors
import kotlin.io.path.absolutePathString

class JavaAppConfigBuilder(private val name: String, private val event: AnActionEvent) {

    private val factory: ConfigurationFactory =
        runConfigurationType<ApplicationConfigurationType>().configurationFactories[0]
    private val appCfg: ApplicationConfiguration = factory.createConfiguration(
        name,
        factory.createTemplateConfiguration(event.project!!)
    ) as ApplicationConfiguration
    private val varRepo: VariableRepository = VariableRepository.getInstance()

    fun setMainClass(value: JsonElement?): JavaAppConfigBuilder {
        val mainClassStr = value?.jsonPrimitive?.content ?: throw ImportException("Main class is not specified")

        if (varRepo.contains(mainClassStr)) {
//            val expMainClassStr = varRepo.expandMacrosInString(mainClassStr, event.dataContext)
//            if (expMainClassStr.count { c -> c == '/' } == 0) {
//                throw ImportException("Main class name in VSCode config $name contains variables that cannot be resolved as a correct argument: $mainClassStr")
//            }
//            mainClassStr = expMainClassStr
            throw ImportException("VSCode config \"mainClass\" property contains variables: $mainClassStr")
        }

        val mainClassPath = Paths.get(mainClassStr)
        val mainClass = if (mainClassPath.isAbsolute) {
            findMainClassFromPath(mainClassPath)
        } else if (mainClassStr.count { c -> c == '/' } == 1 && !mainClassStr.startsWith("/")) {
            appCfg.configurationModule.findClass(mainClassStr.substringAfter('/'))
        } else {
            appCfg.configurationModule.findClass(mainClassStr)
        } ?: throw ImportException("Cannot find specified main class: $mainClassStr")

        appCfg.setMainClass(mainClass)
        return this
    }

    private fun findMainClassFromPath(path: Path): PsiClass {
        val files = FilenameIndex.getVirtualFilesByName(
            path.fileName.toString(),
            GlobalSearchScope.allScope(event.project!!)
        )
        val file = if (!files.isEmpty()) {
            files.first()
        } else {
            LocalFileSystem.getInstance().findFileByIoFile(File(path.absolutePathString()))
                ?: throw ImportException("Main class name in VSCode config $name contains variables that cannot be resolved as a correct argument: ${path.absolutePathString()}")
        }

        val mainFile = PsiManager.getInstance(event.project!!).findFile(file) as? PsiJavaFile
            ?: throw ImportException("Specified main class is not found!")
        for (cl: PsiClass in mainFile.classes) {
            if (PsiMethodUtil.hasMainMethod(cl)) {
                return cl
            }
        }
        throw ImportException("Main class name in VSCode config $name contains variables that cannot be resolved as a correct argument: ${path.absolutePathString()}")
    }

    fun setJavaExec(value: JsonElement?): JavaAppConfigBuilder {
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

        if (!trySetJavaExecStr(javaExecStr))
            throw ImportException("Specified JDK/JRE path is invalid: $javaExecStr")

        return this
    }

    private fun trySetJavaExecStr(value: String): Boolean {
        var javaPath: Path = Paths.get(varRepo.expandMacrosInString(value, event.dataContext))

        while (javaPath.nameCount != 0) {
            try {
                JavaParametersUtil.checkAlternativeJRE(javaPath.toString())
                appCfg.alternativeJrePath = javaPath.toString()
                appCfg.isAlternativeJrePathEnabled = true
                return true
            } catch (exc: RuntimeConfigurationWarning) {
                javaPath = javaPath.parent
            }
        }
        return false
    }

    // ! Advanced Substitution
    fun setProgramArgs(value: JsonElement?): JavaAppConfigBuilder {
        if (value.runCatching { value?.jsonPrimitive != null }.getOrElse { false }) {
            appCfg.programParameters = varRepo.substituteAllVariables(value!!.jsonPrimitive.content)
        } else if (value?.jsonArray != null) {
            appCfg.programParameters = value.jsonArray.stream().map { m ->
                varRepo.substituteAllVariables(m.jsonPrimitive.content)
            }.collect(Collectors.joining(" "))
        }

        return this
    }

    // ! $Auto $Runtime $Test
    // ! Advanced Substitution
    fun setModulePaths(value: JsonElement?): JavaAppConfigBuilder {
        val modulePaths = LinkedList<ModuleBasedConfigurationOptions.ClasspathModification>()

        if (value != null) {
            for (modulePathStr in value.jsonArray) {
                addClassPathModification(modulePaths, modulePathStr, false)
            }
        }

        appCfg.classpathModifications.addAll(modulePaths)
        return this
    }

    // ! $Auto $Runtime $Test
    // ! Advanced Substitution
    fun setClassPaths(value: JsonElement?): JavaAppConfigBuilder {
        val classPaths = LinkedList<ModuleBasedConfigurationOptions.ClasspathModification>()

        if (value != null) {
            for (classPathStr in value.jsonArray) {
                addClassPathModification(classPaths, classPathStr, true)
            }
        }

        appCfg.classpathModifications.addAll(classPaths)
        return this
    }

    private fun addClassPathModification(
        list: LinkedList<ModuleBasedConfigurationOptions.ClasspathModification>,
        path: JsonElement,
        isClassPath: Boolean
    ) {
        val expModuleStr: String = varRepo.expandMacrosInString(path.jsonPrimitive.content, event.dataContext)
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
        val vmArgsBuilder: StringBuilder = StringBuilder()

        if (value.runCatching { value?.jsonPrimitive != null }.getOrElse { false }) {
            val vmArgsStr: String = varRepo.substituteAllVariables(value!!.jsonPrimitive.content)
            vmArgsBuilder.append(vmArgsStr)
        } else if (value?.jsonArray != null) {
            val vmArgs: String = value.jsonArray.stream().map { m ->
                varRepo.substituteAllVariables(m.jsonPrimitive.content)
            }.collect(Collectors.joining(" "))
            vmArgsBuilder.append(vmArgs)
        }

        if (appCfg.vmParameters != null) {
            appCfg.vmParameters += " $vmArgsBuilder"
        } else {
            appCfg.vmParameters = vmArgsBuilder.toString()
        }

        return this
    }

    // ! Advanced Substitution
    fun setWorkingDirectory(value: JsonElement?): JavaAppConfigBuilder {
        if (value?.jsonPrimitive?.content != null) {
            appCfg.workingDirectory = varRepo.substituteAllVariables(value.jsonPrimitive.content)
        } else {
            appCfg.workingDirectory = event.project!!.guessProjectDir()?.path
        }
        return this
    }

    // ! Advanced Substitution
    fun setEnv(value: JsonElement?): JavaAppConfigBuilder {
        if (value != null) {
            val envMap: MutableMap<String, String> = HashMap()
            for (entry in value.jsonObject.entries) {
                envMap[entry.key] = varRepo.substituteAllVariables(entry.value.jsonPrimitive.content)
            }
            appCfg.isPassParentEnvs = true
            appCfg.envs.plusAssign(envMap)
        }

        return this
    }

    fun setEnvFromFile(value: JsonElement?): JavaAppConfigBuilder {
        if (value?.jsonPrimitive?.content != null) {
            val envMap: MutableMap<String, String> = HashMap()
            val envPath: Path = Paths.get(value.jsonPrimitive.content)
            try {
                val dotenv: Dotenv = dotenv {
                    directory = varRepo.expandMacrosInString(envPath.parent.toString(), event.dataContext)
                    filename = varRepo.expandMacrosInString(envPath.fileName.toString(), event.dataContext)
                    systemProperties = false
                }
                for (dotenvEntry in dotenv.entries(Dotenv.Filter.DECLARED_IN_ENV_FILE)) {
                    envMap[dotenvEntry.key] = dotenvEntry.value
                }
            } catch (exc: DotenvException) {
                throw ImportException("Env file is invalid", exc)
            }
            appCfg.isPassParentEnvs = true
            appCfg.envs.plusAssign(envMap)
        }

        return this
    }

    fun setShortenCommandLine(value: JsonElement?): JavaAppConfigBuilder {
        if (value?.jsonPrimitive?.content != null) {
            appCfg.shortenCommandLine = when (value.jsonPrimitive.content) {
                "none" -> ShortenCommandLine.NONE
                "jarmanifest" -> ShortenCommandLine.MANIFEST
                "argfile" -> ShortenCommandLine.ARGS_FILE
                else -> null
            }
        }
        return this
    }

    fun setEncoding(value: JsonElement?): JavaAppConfigBuilder {
        val encoding: String? = value?.jsonPrimitive?.content
        if (encoding != null) {
            appCfg.vmParameters += " -Dfile.encoding=$encoding"
        }
        return this
    }

    fun build(runManager: RunManager): RunnerAndConfigurationSettings {
        appCfg.checkConfiguration()
        return runManager.createConfiguration(appCfg, factory)
    }

}
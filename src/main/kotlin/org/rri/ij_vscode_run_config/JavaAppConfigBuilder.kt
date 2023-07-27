package org.rri.ij_vscode_run_config

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.ShortenCommandLine
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.application.ApplicationConfigurationType
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.runConfigurationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiMethodUtil
import io.github.cdimascio.dotenv.Dotenv
import io.github.cdimascio.dotenv.DotenvException
import io.github.cdimascio.dotenv.dotenv
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.stream.Collectors
import kotlin.io.path.absolutePathString

class JavaAppConfigBuilder(private val name: String, private val event: AnActionEvent, private val json: JsonObject) {

    private val factory: ConfigurationFactory =
        runConfigurationType<ApplicationConfigurationType>().configurationFactories[0]
    private val appCfg: ApplicationConfiguration = factory.createConfiguration(
        name,
        factory.createTemplateConfiguration(event.project!!)
    ) as ApplicationConfiguration
    private val jvmArgPathSeparator: Char = if (SystemInfo.isWindows) ';' else ':'

    // * "mainClass"
    fun setMainClass(): JavaAppConfigBuilder {
        var mainClassStr = json["mainClass"]?.jsonPrimitive?.content!!

        if (VariableRepository.getInstance().contains(mainClassStr)) {
            val expMainClassStr = VariableRepository.getInstance().expandMacrosInString(mainClassStr, event.dataContext)
            if (expMainClassStr.count { c -> c == '/' } == 0) {
                throw ImportException("Main class name in VSCode config $name contains variables that cannot be resolved as a correct filepath: $expMainClassStr")
            }
            mainClassStr = expMainClassStr
        }

        val mainClassPath = Paths.get(mainClassStr)
        val mainClass = if (mainClassPath.isAbsolute) {
            findMainClassFromPath(mainClassPath)
        } else if (mainClassStr.count { c -> c == '/' } == 1 && !mainClassStr.startsWith("/")) {
            appCfg.configurationModule.findClass(mainClassStr.substringAfter('/'))!!
        } else {
            appCfg.configurationModule.findClass(mainClassStr)!!
        }

        appCfg.setMainClass(mainClass)
        return this
    }

    private fun findMainClassFromPath(path: Path): PsiClass {
        val files = FilenameIndex.getVirtualFilesByName(path.fileName.toString(), GlobalSearchScope.allScope(event.project!!))
        val file = if (!files.isEmpty()) {
            files.first()
        } else {
            LocalFileSystem.getInstance().findFileByIoFile(File(path.absolutePathString())) ?: throw ImportException("Main class name in VSCode config $name contains variables that cannot be resolved as a correct filepath: ${path.absolutePathString()}")
        }

        val mainFile = PsiManager.getInstance(event.project!!).findFile(file) as? PsiJavaFile ?: throw ImportException("Specified main class is not found!")
        for (cl: PsiClass in mainFile.classes) {
            if (PsiMethodUtil.hasMainMethod(cl)) {
                return cl
            }
        }
        throw ImportException("Main class name in VSCode config $name contains variables that cannot be resolved as a correct filepath: ${path.absolutePathString()}")
    }

    // & "args"
    // ! Advanced Substitution
    fun setProgramArgs(): JavaAppConfigBuilder {
        if (json["args"] != null) {
            appCfg.programParameters = json["args"]?.jsonArray!!.stream().map { m ->
                VariableRepository.getInstance().substituteAllVariables(m.jsonPrimitive.content)
            }.collect(Collectors.joining(" "))
        }
        return this
    }

    // & "modulePaths"
    // ! $Auto $Runtime $Test
    // ! Exclude path
    // ! Advanced Substitution
    fun setModulePaths(): JavaAppConfigBuilder {
        val stringBuilder: StringBuilder = StringBuilder()

        if (json["modulePaths"] != null) {
            val modulePaths: String = json["modulePaths"]?.jsonArray!!.stream()
                .map{ m -> VariableRepository.getInstance().substituteAllVariables(m.jsonPrimitive.content) }
                .filter{ s -> !s.equals("\$Auto") || !s.equals("\$Runtime") || !s.equals("\$Test") }
                .filter{ s -> s[0] != '!' }
                .collect(Collectors.joining(jvmArgPathSeparator.toString()))
            stringBuilder.append("--module-path $modulePaths")
        }

        if (appCfg.vmParameters != null) {
            appCfg.vmParameters += " $stringBuilder"
        } else {
            appCfg.vmParameters = stringBuilder.toString()
        }

//        val mods = appCfg.classpathModifications ?: LinkedList<ModuleBasedConfigurationOptions.ClasspathModification>()
//
//        if (json["modulePaths"] != null) {
//            for (module in json["modulePaths"]?.jsonArray!!) {
//                val modulePath: String = VariableRepository.getInstance().expandMacrosInString(module.jsonPrimitive.content, event.dataContext)
//                if (modulePath == "\$Auto" || modulePath == "\$Runtime" || modulePath == "\$Test") {
//                    continue
//                } else if (modulePath[0] == '!') {
//                    mods.add(ModuleBasedConfigurationOptions.ClasspathModification(modulePath.substring(1), true))
//                } else {
//                    mods.add(ModuleBasedConfigurationOptions.ClasspathModification(modulePath, false))
//                }
//            }
//        }
//
//        appCfg.classpathModifications = mods

        return this
    }

    // & "classPaths"
    // ! $Auto $Runtime $Test
    // ! Exclude path
    // ! Advanced Substitution
    fun setClassPaths(): JavaAppConfigBuilder {
        val stringBuilder: StringBuilder = StringBuilder()

        if (json["classPaths"] != null) {
            val classPaths : String = json["classPaths"]?.jsonArray!!.stream()
                .map{ m -> VariableRepository.getInstance().substituteAllVariables(m.jsonPrimitive.content) }
                .filter{ s -> !s.equals("\$Auto") || !s.equals("\$Runtime") || !s.equals("\$Test") }
                .filter{ s -> s[0] != '!' }
                .collect(Collectors.joining(jvmArgPathSeparator.toString()))
            stringBuilder.append("--class-path \$Classpath\$$jvmArgPathSeparator$classPaths")
        }

        if (appCfg.vmParameters != null) {
            appCfg.vmParameters += " $stringBuilder"
        } else {
            appCfg.vmParameters = stringBuilder.toString()
        }

//        val classes =
//            appCfg.classpathModifications ?: LinkedList<ModuleBasedConfigurationOptions.ClasspathModification>()
//
//        if (json["classPaths"] != null) {
//            for (cl in json["classPaths"]?.jsonArray!!) {
//                val classPath: String =
//                    VariableRepository.getInstance().expandMacrosInString(cl.jsonPrimitive.content, event.dataContext)
//                if (classPath == "\$Auto" || classPath == "\$Runtime" || classPath == "\$Test") {
//                    continue
//                } else if (classPath[0] == '!') {
//                    classes.add(ModuleBasedConfigurationOptions.ClasspathModification(classPath.substring(1), true))
//                } else {
//                    classes.add(ModuleBasedConfigurationOptions.ClasspathModification(classPath, false))
//                }
//            }
//        }
//
//        appCfg.classpathModifications = classes

        return this
    }

    // & "vmArgs"
    // ! Advanced Substitution
    fun setVMArgs(): JavaAppConfigBuilder {
        val vmArgsBuilder: StringBuilder = StringBuilder()

        if (json["vmArgs"] != null && try {
                json["vmArgs"]?.jsonPrimitive != null
            } catch (exc: IllegalArgumentException) {
                false
            }
        ) {
            val vmArgsStr: String =
                VariableRepository.getInstance().substituteAllVariables(json["vmArgs"]!!.jsonPrimitive.content)
            vmArgsBuilder.append(" $vmArgsStr ")
        } else if (json["vmArgs"]?.jsonArray != null) {
            val vmArgs: String = json["vmArgs"]?.jsonArray!!.stream().map { m ->
                VariableRepository.getInstance().substituteAllVariables(m.jsonPrimitive.content)
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

    // & "cwd"
    // ! Advanced Substitution
    fun setWorkingDirectory(): JavaAppConfigBuilder {
        if (json["cwd"]?.jsonPrimitive?.content != null) {
            appCfg.workingDirectory =
                VariableRepository.getInstance().substituteAllVariables(json["cwd"]!!.jsonPrimitive.content)
        } else {
            appCfg.workingDirectory = event.project!!.guessProjectDir()?.path
        }
        return this
    }

    // * "env"
    fun setEnv(): JavaAppConfigBuilder {
        val envMap: MutableMap<String, String> = HashMap()
        if (json["env"] != null) {
            for (entry in json["env"]?.jsonObject?.entries!!) {
                envMap[entry.key] =
                    VariableRepository.getInstance().substituteAllVariables(entry.value.jsonPrimitive.content)
            }
        }

        if (json["envFile"] != null && json["envFile"]?.jsonPrimitive?.content != null) {
            val envPath: Path = Paths.get(json["envFile"]?.jsonPrimitive?.content!!)
            try {
                val dotenv: Dotenv = dotenv {
                    directory = VariableRepository.getInstance()
                        .expandMacrosInString(envPath.parent.toString(), event.dataContext)
                    filename = VariableRepository.getInstance()
                        .expandMacrosInString(envPath.fileName.toString(), event.dataContext)
//                            systemProperties = false
                }
                for (dotenvEntry in dotenv.entries()) {
                    envMap[dotenvEntry.key] = dotenvEntry.value
                }
            } catch (exc: DotenvException) {
                println(exc)
            }
        }

        appCfg.isPassParentEnvs = true
        appCfg.envs.plusAssign(envMap)

        return this
    }

    // * "shortenCommandLine"
    fun setShortenCommandLine(): JavaAppConfigBuilder {
        if (json["shortenCommandLine"] != null && json["shortenCommandLine"]?.jsonPrimitive?.content != null) {
            appCfg.shortenCommandLine = when (json["shortenCommandLine"]?.jsonPrimitive?.content) {
                "none" -> ShortenCommandLine.NONE
                "jarmanifest" -> ShortenCommandLine.MANIFEST
                "argfile" -> ShortenCommandLine.ARGS_FILE
                else -> null
            }
        }
        return this
    }

    fun setEncoding(): JavaAppConfigBuilder {
        val encoding: String? = json["encoding"]?.jsonPrimitive?.content
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
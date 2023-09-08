package org.rri.ij_vscode_run_config.logic

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.compound.CompoundRunConfiguration
import com.intellij.execution.compound.CompoundRunConfigurationOptions
import com.intellij.execution.compound.CompoundRunConfigurationType
import com.intellij.execution.impl.RunConfigurationBeforeRunProvider
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.JDOMUtil
import kotlinx.serialization.json.*
import org.jdom.Element
import org.rri.ij_vscode_run_config.Dependencies
import org.rri.ij_vscode_run_config.DependsOrder
import org.rri.ij_vscode_run_config.RunnableHolder
import org.rri.ij_vscode_run_config.isMavenCommand
import org.rri.ij_vscode_run_config.logic.builders.configs.CompoundConfigBuilder
import org.rri.ij_vscode_run_config.logic.builders.configs.JavaAppConfigBuilder
import org.rri.ij_vscode_run_config.logic.builders.configs.JavaRemoteConfigBuilder
import org.rri.ij_vscode_run_config.logic.builders.tasks.GradleConfigBuilder
import org.rri.ij_vscode_run_config.logic.builders.tasks.MavenConfigBuilder
import org.rri.ij_vscode_run_config.logic.builders.tasks.ShellConfigBuilder
import java.util.*

const val UNNAMED_CONFIGURATION_PROBLEM_NAME: String = "UNNAMED_CONFIGURATION_PROBLEM_NAME"
const val PLUGIN_CONFIGURATION_NAME_SUFFIX: String = " (imported from vscode by plugin)"
const val CONFIGURATION_NO_DEPENDENCIES_SUFFIX: String = " (no dependencies)"
const val COMPOUND_CONFIGURATION_WITH_PRE_LAUNCH_TASK_SUFFIX: String = " (post launch configuration)"


class ImportManager(private val project: Project, private val context: DataContext) {

    private val runManager = RunManager.getInstance(project)
    private val beforeRunTaskProvider = RunConfigurationBeforeRunProvider(project)

    // maven is in shell|process type
    private val supportedTaskTypes: Set<String> = setOf("shell", "process", "gradle")
    private val supportedConfigTypes: Set<String> = setOf("java")

    private val configurations: MutableMap<String, RunnableHolder> = HashMap()

    fun getConfigurationNames(): Set<String> = configurations.keys

    fun getConfigurations(): Map<String, RunnableHolder> = configurations

    fun deserialize(): Map<String, ImportProblems> {
        val problems: MutableMap<String, ImportProblems> = HashMap()
        deserializeTasks(problems)
        deserializeSingleConfigs(problems)

        val configurationNames = configurations.keys.toSet()

        for (name in configurationNames) {
            try {
                dispatchDependencies(name, name, problems)
            } catch (exc: Throwable) {
                addProblem(problems, name, exc)
            }
        }

        deserializeCompoundConfigs(problems)

        return problems
    }

    private fun dispatchDependencies(root: String, curr: String, problems: MutableMap<String, ImportProblems>) {
        if (configurations[curr] == null) {
            val it = configurations.iterator()
            while (it.hasNext()) {
                val (name, holder) = it.next()
                if (name== root
                    || holder.dependencies.all.contains(curr)
                    || holder.dependencies.all.contains(root)
                    || holder.dependencies.onlyCurrent.contains(curr)
                    || holder.dependencies.onlyCurrent.contains(root))
                {
                    it.remove()
                }
            }
            throw ImportError("Invalid dependency $curr in $root configuration")
        }

        for (cfg in configurations[curr]!!.dependencies.onlyCurrent) {
            configurations[root]?.dependencies?.all?.add(cfg)
            dispatchDependencies(root, cfg, problems)
        }
    }

    private fun deserializeTasks(problems: MutableMap<String, ImportProblems>) {
        val tasksJson = project.service<VSCodeFolderService>().getTasksJson()?.jsonObject ?: return
        val taskArray = tasksJson["tasks"]?.jsonArray?.map { j -> j.jsonObject } ?: return
        val settingsJson = project.service<VSCodeFolderService>().getSettingsJson()?.jsonObject

        for (taskObj: JsonObject in taskArray) {
            var taskName: String? = null
            try {
                taskName = taskObj["label"]?.jsonPrimitive?.content
                if (taskName == null || VariableRepository.contains(taskName))
                    throw ImportWarning("Invalid task name in $taskObj")

                if (!supportedTaskTypes.contains(taskObj["type"]?.jsonPrimitive?.content))
                    throw ImportWarning("Unsupported task type")

                val taskSettings: RunnerAndConfigurationSettings = createTask(taskName, taskObj, settingsJson)
                val dependencies: Dependencies = createTaskDependencies(taskObj)
                configurations[taskName] = RunnableHolder(taskSettings, dependencies)
            } catch (exc: Throwable) {
                addProblem(problems, taskName, exc)
            }
        }
    }

    private fun deserializeSingleConfigs(problems: MutableMap<String, ImportProblems>) {
        val launchJson = project.service<VSCodeFolderService>().getLaunchJson()?.jsonObject ?: return
        val configurationArray = launchJson["configurations"]?.jsonArray?.map { j -> j.jsonObject } ?: return
        val settingsJson = project.service<VSCodeFolderService>().getSettingsJson()?.jsonObject

        for (cfgJson: JsonObject in configurationArray) {
            var configName: String? = null
            try {
                configName = cfgJson["name"]?.jsonPrimitive?.content
                if (configName == null || VariableRepository.contains(configName))
                    throw ImportWarning("Invalid configuration name in $cfgJson")

                if (!supportedConfigTypes.contains(cfgJson["type"]?.jsonPrimitive?.content))
                    throw ImportWarning("Unsupported configuration type")

                val configSettings: RunnerAndConfigurationSettings =
                    createConfiguration(configName, cfgJson, settingsJson)
                val dependencies: Dependencies = createConfigurationDependencies(cfgJson)
                configurations[configName] = RunnableHolder(configSettings, dependencies)
            } catch (exc: Throwable) {
                addProblem(problems, configName, exc)
            }
        }
    }

    private fun deserializeCompoundConfigs(problems: MutableMap<String, ImportProblems>) {
        val launchJson = project.service<VSCodeFolderService>().getLaunchJson()?.jsonObject ?: return
        val compoundArray = launchJson["compounds"]?.jsonArray?.map { j -> j.jsonObject } ?: return

        for (compoundJson: JsonObject in compoundArray) {
            var compoundName: String? = null
            try {
                compoundName = compoundJson["name"]?.jsonPrimitive?.content
                if (compoundName == null || VariableRepository.contains(compoundName))
                    throw ImportWarning("Invalid compound name in $compoundJson")

                val jsonArray: JsonArray = compoundJson["configurations"].runCatching { this?.jsonArray }.getOrNull()
                    ?: throw ImportError("Invalid compound configuration")
                val currentConfigNames: List<String> =
                    jsonArray.runCatching { this.map { it.jsonPrimitive.content } }.getOrNull()
                        ?: throw ImportError("Invalid compound configuration")

                val compoundSettings: RunnerAndConfigurationSettings =
                    CompoundConfigBuilder(compoundName, project)
                        .setConfigurations(currentConfigNames, configurations)
                        .build(runManager)

                val allDependencies: MutableSet<String> = LinkedHashSet()
                val onlyCurrentDependencies: MutableSet<String> = LinkedHashSet()
                for (configName in currentConfigNames) {
                    allDependencies.addAll(configurations[configName]!!.dependencies.all)
                    allDependencies.add(configName)
                    onlyCurrentDependencies.add(configName)
                }

                val preLaunchTaskName: String? = compoundJson["preLaunchTask"]?.jsonPrimitive?.content
                if (preLaunchTaskName == null) {
                    configurations[compoundName] = RunnableHolder(
                        compoundSettings,
                        Dependencies(allDependencies, onlyCurrentDependencies, DependsOrder.PARALLEL)
                    )
                } else {
                    throw ImportError("preLaunchTask is unsupported in Compound configurations")

//                    if (!configurations.contains(preLaunchTaskName)) {
//                        throw ImportError("Invalid preLaunchTask $preLaunchTaskName")
//                    }
//
//                    val newCompoundName: String = compoundName + COMPOUND_CONFIGURATION_WITH_PRE_LAUNCH_TASK_SUFFIX
//                    configurations[newCompoundName] = RunnableHolder(
//                        compoundSettings,
//                        Dependencies(allDependencies.toMutableSet(), onlyCurrentDependencies, DependsOrder.PARALLEL)
//                    )
//
//                    val dummyShellSettings = ShellConfigBuilder(compoundName, project).build(runManager)
//                    val beforeRunTasks: MutableList<BeforeRunTask<*>> = LinkedList()
//
//                    val preLaunchTask = beforeRunTaskProvider.createTask(configurations[preLaunchTaskName]!!.settings.configuration)
//                    preLaunchTask!!.setSettingsWithTarget(configurations[preLaunchTaskName]!!.settings, null)
//                    beforeRunTasks.add(preLaunchTask)
//
//                    val configurationTask = beforeRunTaskProvider.createTask(compoundSettings.configuration)
//                    configurationTask!!.setSettingsWithTarget(compoundSettings, null)
//                    beforeRunTasks.add(configurationTask)
//
//                    dummyShellSettings.configuration.beforeRunTasks = beforeRunTasks
//                    allDependencies.add(preLaunchTaskName)
//                    allDependencies.add(newCompoundName)
//                    configurations[compoundName] = RunnableHolder(
//                        dummyShellSettings,
//                        Dependencies(allDependencies, mutableSetOf(preLaunchTaskName, newCompoundName), DependsOrder.PARALLEL)
//                    )
                }
            } catch (exc: Throwable) {
                addProblem(problems, compoundName, exc)
            }
        }
    }

    private fun addProblem(problems: MutableMap<String, ImportProblems>, configName: String?, exc: Throwable) {
        if (configName == null)
            return

        if (!problems.contains(configName))
            problems[configName] = ImportProblems()

        when (exc) {
            is ImportWarning -> problems[configName]!!.addWarning(exc)
            is ImportError -> problems[configName]!!.addError(exc)
            else -> problems[configName]!!.addError(ImportError(exc.message ?: "Unexpected error", exc))
        }
    }

    fun importAllConfigurations(): Map<String, ImportProblems> {
        return importConfigurationsWithDependencies(getConfigurationNames())
    }

    fun importConfigurationsWithDependencies(configNamesToProcess: Set<String>): Map<String, ImportProblems> {
        val problems: MutableMap<String, ImportProblems> = HashMap()
        var currConfigs: MutableMap<String, RunnableHolder> = HashMap()

        for (configName in configNamesToProcess) {
            if (!configurations.contains(configName))
                addProblem(problems, configName, ImportError("No such configuration"))
            else
                currConfigs[configName] = configurations[configName]!!.deepCopy()
        }

        while (true) {
            val badDependencies: MutableSet<String> = HashSet()
            for ((name, holder) in currConfigs) {
                for (dependency in holder.dependencies.all) {
                    if (!currConfigs.contains(dependency)) {
                        addProblem(problems, name, ImportError("Invalid dependency $dependency"))
                        badDependencies.add(name)
                        badDependencies.add(dependency)
                    }
                }
            }

            if (badDependencies.isEmpty())
                break

            currConfigs = currConfigs.filter {
                !badDependencies.contains(it.key) && it.value.dependencies.all.intersect(badDependencies).isEmpty()
            }.toMutableMap()
        }

        val processedSettings: MutableMap<String, RunnableHolder> = LinkedHashMap()
        try {
            while (currConfigs.isNotEmpty()) {
                val itConfigs: Set<String> = currConfigs.filter { it.value.dependencies.all.isEmpty() }.keys

                for (configName in itConfigs) {
                    val holder: RunnableHolder = currConfigs[configName]!!

                    if (holder.dependencies.onlyCurrent.isNotEmpty()) {
                        if (holder.dependencies.dependsOrder == DependsOrder.SEQUENCE) {
                            val beforeRunTasks: MutableList<BeforeRunTask<*>> = LinkedList()
                            for (dependency in holder.dependencies.onlyCurrent) {
                                val task = beforeRunTaskProvider.createTask(holder.settings.configuration) ?: continue
                                task.setSettingsWithTarget(processedSettings[dependency]!!.settings, null)
                                beforeRunTasks.add(task)
                            }

                            if (beforeRunTasks.isNotEmpty())
                                holder.settings.configuration.beforeRunTasks = beforeRunTasks

                        } else if (holder.dependencies.dependsOrder == DependsOrder.PARALLEL && holder.settings.type !is CompoundRunConfigurationType) {
                            val parallelTasks: MutableSet<String> = holder.dependencies.onlyCurrent.toMutableSet()
                            val noDependenciesName = "$configName$PLUGIN_CONFIGURATION_NAME_SUFFIX$CONFIGURATION_NO_DEPENDENCIES_SUFFIX"
                            parallelTasks.add(noDependenciesName)
                            processedSettings[noDependenciesName] = holder.deepCopy()
                            processedSettings[noDependenciesName]!!.settings.name = noDependenciesName
                            holder.settings = CompoundConfigBuilder(configName, project)
                                .setConfigurations(parallelTasks, processedSettings)
                                .build(runManager)
                        }
                    }

                    val existingSettings: RunnerAndConfigurationSettings? = checkIfSettingsAlreadyExist(holder.settings)
                    if (existingSettings != null) {
                        addProblem(
                            problems,
                            configName,
                            ImportWarning("Configuration already exists with name: $existingSettings")
                        )
                        holder.settings = existingSettings
                    } else {
                        holder.settings.name += PLUGIN_CONFIGURATION_NAME_SUFFIX
                        if (runManager.findSettings(holder.settings.configuration) != null) {
                            holder.settings.name = runManager.suggestUniqueName(holder.settings.name, holder.settings.type)
                        }
                    }

                    for (it in currConfigs) {
                        it.value.dependencies.all.remove(configName)
                    }

                    processedSettings[configName] = currConfigs[configName]!!
                    currConfigs.remove(configName)
                }
            }

        } catch (exc: Throwable) {
            addProblem(problems, UNNAMED_CONFIGURATION_PROBLEM_NAME, exc)
            return problems
        }

        //TODO platform specific properties

        for ((_, holder) in processedSettings) {
            holder.settings.storeInDotIdeaFolder()
            runManager.addConfiguration(holder.settings)
            if (runManager.selectedConfiguration == null) {
                runManager.selectedConfiguration = holder.settings
            }
        }

        return problems
    }

    fun importConfigurations(configNamesToProcess: Set<String>): Map<String, ImportProblems> {
        val problems: MutableMap<String, ImportProblems> = HashMap()
        val currConfigs: MutableSet<String> = HashSet()

        for (configName in configNamesToProcess) {
            if (!configurations.contains(configName)) {
                addProblem(problems, configName, ImportError("No such configuration"))
            } else {
                currConfigs.add(configName)
            }
        }

        for (configName in currConfigs) {
            val configSettings = configurations[configName]!!.settings
            val existingSettings: RunnerAndConfigurationSettings? = checkIfSettingsAlreadyExist(configSettings)
            if (existingSettings != null) {
                addProblem(
                    problems,
                    configName,
                    ImportWarning("Configuration already exists with name: $existingSettings")
                )
                continue
            } else {
                configSettings.name += PLUGIN_CONFIGURATION_NAME_SUFFIX
                if (runManager.findSettings(configSettings.configuration) != null) {
                    configSettings.name = runManager.suggestUniqueName(configSettings.name, configSettings.type)
                }
            }

            //TODO platform specific properties

            configSettings.storeInDotIdeaFolder()
            runManager.addConfiguration(configSettings)
            if (runManager.selectedConfiguration == null) {
                runManager.selectedConfiguration = configSettings
            }
        }

        return problems
    }

    private fun createConfiguration(
        name: String,
        configJson: JsonObject,
        settingsJson: JsonObject?
    ): RunnerAndConfigurationSettings {

        when (val type: String? = configJson["type"]?.jsonPrimitive?.content) {
            "java" -> return when (val request: String? = configJson["request"]?.jsonPrimitive?.content) {
                "launch" -> JavaAppConfigBuilder(name, project)
                    .setMainClass(configJson["mainClass"])
                    .setJavaExec(configJson["javaExec"] ?: settingsJson?.get("java.configuration.runtimes"), context)
                    .setProgramArgs(configJson["args"])
                    .setModulePaths(configJson["modulePaths"], context)
                    .setClassPaths(configJson["classPaths"], context)
                    .setVMArgs(configJson["vmArgs"] ?: settingsJson?.get("java.debug.settings.vmArgs"))
                    .setWorkingDirectory(configJson["cwd"])
                    .setEnv(configJson["env"])
                    .setEnvFromFile(configJson["envFile"], context)
                    .setShortenCommandLine(configJson["shortenCommandLine"])
                    .setEncoding(configJson["encoding"])
                    .build(runManager)

                "attach" -> JavaRemoteConfigBuilder(name, project)
                    .setHostName(configJson["hostName"])
                    .setPort(configJson["port"])
                    .build(runManager)

                else -> throw ImportError("Invalid Java request type $request in configuration $name")
            }

            null -> throw ImportError("Invalid configuration type in configuration $name")
            else -> throw ImportError("Unsupported configuration type $type in configuration $name")
        }

    }

    private fun createTask(
        name: String,
        taskJson: JsonObject,
        settingsJson: JsonObject?
    ): RunnerAndConfigurationSettings {
        //TODO add properties from settingsJson

        val taskOptionsJson: JsonObject? = runCatching { taskJson["options"]?.jsonObject }.getOrNull()
        val taskShellParamsJson: JsonObject? = runCatching { taskOptionsJson?.get("shell")?.jsonObject }.getOrNull()

        return when (val taskType: String? = taskJson["type"]?.jsonPrimitive?.content) {
            "shell", "process" -> if (isMavenCommand(taskJson["command"]?.jsonPrimitive?.content)) {
                MavenConfigBuilder(name, project)
                    .setCommandLine(taskJson["command"], context)
                    .setArgs(taskJson["args"], context)
                    .setWorkingDirectory(taskOptionsJson?.get("cwd"), context)
                    .setEnv(taskOptionsJson?.get("env"), context)
                    .build(runManager)
            } else {
                ShellConfigBuilder(name, project)
                    .setCommand(taskJson["command"], context)
                    .setWorkingDirectory(taskOptionsJson?.get("cwd"), context)
                    .setScriptOptions(taskJson["args"], context)
                    .setEnv(taskOptionsJson?.get("env"), context)
                    .setInterpreterPath(taskShellParamsJson?.get("executable"), context)
                    .setInterpreterOptions(taskShellParamsJson?.get("args"), context)
                    .setIsTerminal(taskType == "shell")
                    .build(runManager)
            }

            "gradle" -> GradleConfigBuilder(name, project)
                .setScript(taskJson["script"], context)
                .setSubProject(taskJson["project"], context)
                .setRootProject(taskJson["rootProject"], context)
                .setProjectFolder(taskJson["projectFolder"], context)
                .setArgs(taskJson["args"], context)
                .setEnv(taskOptionsJson?.get("env"), context)
                .setJavaDebugFlag(taskJson["javaDebug"]?.jsonPrimitive?.content == "true")
                .build(runManager)

            else -> throw ImportWarning("Unsupported task type: $taskType")
        }
    }

    private fun createConfigurationDependencies(configJson: JsonObject): Dependencies {
        val taskName: String? = runCatching { configJson["preLaunchTask"]?.jsonPrimitive?.content }.getOrNull()
        val isValidTask: Boolean = runCatching { configurations[taskName] != null }.getOrElse { false }
        val currDeps: MutableSet<String> = if (isValidTask) Collections.singleton(taskName) else LinkedHashSet()
        return Dependencies(LinkedHashSet(), currDeps, DependsOrder.SEQUENCE)
    }

    private fun createTaskDependencies(taskObj: JsonObject): Dependencies {
        val currTaskDependencies: MutableSet<String> = LinkedHashSet()
        if (runCatching { taskObj["dependsOn"]?.jsonArray != null }.getOrElse { false }) {
            for (currTask in taskObj["dependsOn"]!!.jsonArray) {
                currTaskDependencies.add(currTask.jsonPrimitive.content)
            }
        } else if (taskObj["dependsOn"]?.jsonPrimitive?.content != null) {
            currTaskDependencies.add(taskObj["dependsOn"]!!.jsonPrimitive.content)
        }

        // PARALLEL for tasks by default
        var dependsOrder: DependsOrder = DependsOrder.PARALLEL
        if (runCatching { taskObj["dependsOrder"]?.jsonPrimitive?.content == "sequence" }.getOrElse { false }) {
            dependsOrder = DependsOrder.SEQUENCE
        }

        return Dependencies(LinkedHashSet(), currTaskDependencies, dependsOrder)
    }

    private fun checkIfSettingsAlreadyExist(settings: RunnerAndConfigurationSettings): RunnerAndConfigurationSettings? {
        if (settings.configuration.type is CompoundRunConfigurationType) {
            val thisState: CompoundRunConfigurationOptions = (settings.configuration as CompoundRunConfiguration).state
            for (currentSettings in runManager.getConfigurationSettingsList(settings.configuration.type)) {
                if (currentSettings.configuration.type is CompoundRunConfigurationType
                    && thisState.configurations == (currentSettings.configuration as CompoundRunConfiguration).state.configurations) {
                    return currentSettings
                }
            }
            return null
        }

        val thisElement = Element("configuration")
        settings.configuration.writeExternal(thisElement)

        for (currentSettings in runManager.getConfigurationSettingsList(settings.configuration.type)) {
            val thatElement = Element("configuration")
            currentSettings.configuration.writeExternal(thatElement)

            if (JDOMUtil.areElementsEqual(thisElement, thatElement))
                return currentSettings
        }

        return null
    }

}

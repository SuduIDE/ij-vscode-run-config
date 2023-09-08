package org.rri.ij_vscode_run_config.ux

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.compound.CompoundRunConfigurationType
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.CheckboxTree
import com.intellij.ui.CheckboxTreeBase
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.ui.roots.ToolbarPanel
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.tree.TreeUtil
import org.rri.ij_vscode_run_config.RunnableHolder
import org.rri.ij_vscode_run_config.logic.ImportProblems
import java.awt.BorderLayout
import java.awt.ScrollPane
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import java.util.*
import javax.swing.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet
import kotlin.reflect.KClass


class ImportDialogWrapper(
    private val project: Project,
    private val configurations: Map<String, RunnableHolder>,
    private val problems: Map<String, ImportProblems>
) : DialogWrapper(project, true) {

    private val myCheckPolicy = CheckboxTreeBase.CheckPolicy(true, false, false, true)

    private lateinit var myScrollPane: JScrollPane

    private lateinit var myPanel: JPanel

    private lateinit var myConfigTree: CheckboxTree
    private lateinit var myConfigList: CheckboxTree

    private var isTreeView: Boolean = true

    init {
        title = "Import VSCode Run Configurations"
        super.init()
    }

    fun getIsTreeView() : Boolean {
        return isTreeView
    }

    fun getSelectedConfigurations() : Set<String> {
        val result: MutableSet<String> = HashSet()
        if (isTreeView) {
            val filter = Tree.NodeFilter<CheckedTreeNode> {
                it.isChecked && (it.parent != null) && !(it.parent as CheckedTreeNode).isChecked
            }
            val nodes: ArrayList<out CheckedTreeNode> = myConfigList.getCheckedNodes(CheckedTreeNode::class.java, filter).toCollection(ArrayList())
            for (node in nodes) {
                val userObject = node.userObject
                if (userObject is RunnerAndConfigurationSettings) {
                    result.add(userObject.name)
                }
            }
        } else {
            val tmp: List<Any> = TreeUtil.collectSelectedUserObjects(myConfigList)

            val nodes: ArrayList<out CheckedTreeNode> = myConfigList.getCheckedNodes(CheckedTreeNode::class.java, null).toCollection(ArrayList())
            for (node in nodes) {
                val userObject = node.userObject
                if (userObject is RunnerAndConfigurationSettings) {
                    result.add(userObject.name)
                }
            }
        }

        return result
    }

    private fun addConfigurationCheckBoxNode(config: RunnableHolder, parent: CheckedTreeNode) {
        val currentNode = CheckedTreeNode(config.settings)
        for (child in config.dependencies.onlyCurrent) {
            addConfigurationCheckBoxNode(configurations[child]!!, currentNode)
        }

        parent.add(currentNode)
    }

    private fun createTree(): CheckboxTree {
        val root = CheckedTreeNode("treeViewRoot")
        root.isChecked = false

        val sortedBranches: MutableSet<String> = TreeSet { s1, s2 ->
            configurations[s1]?.settings.toString().compareTo(
                configurations[s2]?.settings.toString()
            )
        }
        sortedBranches.addAll(configurations.keys)

        for ((_, configHolder) in configurations) {
            sortedBranches.removeAll(configHolder.dependencies.onlyCurrent)
        }

        for (configName in sortedBranches) {
            addConfigurationCheckBoxNode(configurations[configName]!!, root)
        }

        val result = CheckboxTree(createRenderer(), root, myCheckPolicy)
        TreeUtil.expandAll(result)

        return result
    }

    private fun createList(): CheckboxTree {
        val root = CheckedTreeNode("listViewRoot")
        root.isChecked = false

        val sortedNodes: MutableSet<String> = TreeSet { s1, s2 ->
            configurations[s1]?.settings.toString().compareTo(
                configurations[s2]?.settings.toString()
            )
        }

        for ((name, holder) in configurations) {
            if (holder.settings.type !is CompoundRunConfigurationType) {
                sortedNodes.add(name)
            }
        }

        for (configName in sortedNodes) {
            val currentNode = CheckedTreeNode(configurations[configName]!!.settings)
            root.add(currentNode)
        }
        return CheckboxTree(createRenderer(), root, myCheckPolicy)
    }

    private inner class RefreshAction(text: String, icon: Icon) : AnAction({text}, icon) {
        override fun actionPerformed(e: AnActionEvent) {
            TODO("Not yet implemented")
        }
    }

    private inner class ToggleConfigDependenciesAction(text: String, icon: Icon) : AnAction({text}, icon), Toggleable {

        init {
            Toggleable.setSelected(templatePresentation, isTreeView)
        }

        override fun actionPerformed(e: AnActionEvent) {
            if (Toggleable.isSelected(e.presentation)) {
                myPanel.remove(myScrollPane)
                myScrollPane = ScrollPaneFactory.createScrollPane(myConfigList)
                myPanel.add(myScrollPane)
                isTreeView = false
            } else {
                myPanel.remove(myScrollPane)
                myScrollPane = ScrollPaneFactory.createScrollPane(myConfigTree)
                myPanel.add(myScrollPane)
                isTreeView = true
            }
            Toggleable.setSelected(e.presentation, isTreeView)
            myPanel.revalidate()
            myPanel.repaint()
        }
    }

    private inner class ExpandTreeAction(text: String, icon: Icon) : AnAction({text}, icon) {
        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = isTreeView
            super.update(e)
        }

        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

        override fun actionPerformed(e: AnActionEvent) {
            TreeUtil.expandAll(myConfigTree)
        }
    }

    private inner class CollapseTreeAction(text: String, icon: Icon) : AnAction({text}, icon) {
        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = isTreeView
            super.update(e)
        }

        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

        override fun actionPerformed(e: AnActionEvent) {
            TreeUtil.collapseAll(myConfigTree, 0)
        }
    }

    private inner class ShowProblems(text: String, icon: Icon) : AnAction({text}, icon) {
        override fun actionPerformed(e: AnActionEvent) {
            TODO("Not yet implemented")
        }
    }

    override fun createCenterPanel(): JComponent {
        myPanel = JPanel(BorderLayout(0, 0))
        myPanel.border = JBUI.Borders.empty()

        myConfigTree = createTree()
        myConfigList = createList()

        val actionGroup = createActionGroup()

        val toolBar: ActionToolbar = ActionManager.getInstance().createActionToolbar("ImportDialogWrapper", actionGroup, true)
        toolBar.targetComponent = myPanel

        val toolbarPanel = JPanel(BorderLayout())
        toolbarPanel.add(toolBar.component, BorderLayout.CENTER)

        val myNorthPanel = JPanel(BorderLayout())
        myNorthPanel.add(toolbarPanel, BorderLayout.NORTH)

        myPanel.add(myNorthPanel, BorderLayout.NORTH)

        myScrollPane = ScrollPaneFactory.createScrollPane(myConfigTree)

        myPanel.add(myScrollPane, BorderLayout.CENTER)

        return myPanel
    }

    private fun createActionGroup() : ActionGroup {
        val actionGroup = DefaultActionGroup()
        actionGroup.add(ToggleConfigDependenciesAction("Dependencies", AllIcons.Ide.UpDown))
        actionGroup.add(ExpandTreeAction("Expand All Dependencies", AllIcons.Actions.Expandall))
        actionGroup.add(CollapseTreeAction("Collapse All Dependencies", AllIcons.Actions.Collapseall))
        actionGroup.addSeparator()
        actionGroup.add(RefreshAction("Refresh", AllIcons.Actions.Refresh))
        actionGroup.addSeparator()
        actionGroup.add(ShowProblems("Show Problems", AllIcons.Scope.Problems))

        return actionGroup
    }

    private fun createRenderer() = object : CheckboxTree.CheckboxTreeCellRenderer(true, false) {
        override fun customizeRenderer(
            t: JTree,
            value: Any,
            selected: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            focus: Boolean
        ) {
            if (value !is CheckedTreeNode) return

            val userObject = value.userObject
            if (userObject is String) {
                textRenderer.append(userObject)
            }
            if (userObject is RunnerAndConfigurationSettings) {
//                if (value.children().hasMoreElements()) {
//                    textRenderer.icon = AllIcons.RunConfigurations.Compound
//                } else {
                    textRenderer.icon = userObject.configuration.icon
//                }
                textRenderer.append(userObject.name)
            }
        }
    }

}

package fr.openium.autocommit.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.ui.CommitMessage
import git4idea.GitBranch
import git4idea.repo.GitRepositoryManager
import git4idea.repo.isSubmodule


class FillCommitMessageAction : DumbAwareAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.EDT

    override fun actionPerformed(anActionEvent: AnActionEvent) {
        anActionEvent.project?.let { project ->
            getCommitMessageField(anActionEvent)?.apply {
                val shouldMoveCaret = editorField.text.isBlank()
                setCommitMessage(
                    getFormattedCommitMessage(
                        branchName = getCurrentBranch(project).name,
                        text = editorField.text.ifBlank { "" },
                    )
                )
                if (shouldMoveCaret) {
                    editorField.setCaretPosition(editorField.text.length)
                }
            }
        }
    }

    private fun getCommitMessageField(anActionEvent: AnActionEvent): CommitMessage? {
        return anActionEvent.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL) as CommitMessage?
    }

    private fun getCurrentBranch(project: Project): GitBranch {
        return GitRepositoryManager.getInstance(project).repositories.firstOrNull {
            !it.isSubmodule()
        }?.currentBranch ?: error("Branch not found")
    }

    private fun getFormattedCommitMessage(
        branchName: String,
        text: String,
    ): String {
        return BRANCH_FORMAT.toRegex().find(branchName)?.let {
            val (branchType, ticket) = it.destructured
            val branchTypeFormatted = branchType.lowercase()
            val commitType = typeSynonyms[branchTypeFormatted] ?: branchTypeFormatted
            val commitText = COMMIT_FORMAT.toRegex().find(text)?.groupValues?.lastOrNull() ?: text
            "$commitType(#$ticket): $commitText"
        } ?: text
    }

    companion object {
        const val BRANCH_FORMAT = """([^/]+)/([^_]+)"""
        const val COMMIT_FORMAT = """[^()]+\(#[^()]+\): ?(.+)?"""

        val typeSynonyms = mapOf(
            "feature" to "feat",
            "bugfix" to "fix",
        )
    }
}
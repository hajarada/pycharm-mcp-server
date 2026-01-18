package com.github.pycharm.refactoring.refactoring

import com.github.pycharm.refactoring.server.models.FileChange
import com.github.pycharm.refactoring.server.models.RenameRequest
import com.github.pycharm.refactoring.server.models.RenameResponse
import com.github.pycharm.refactoring.util.ProjectUtils
import com.github.pycharm.refactoring.util.PsiUtils
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.rename.RenameProcessor

class RenameService {

    fun rename(request: RenameRequest): RenameResponse {
        val project = ProjectUtils.findProjectByPath(request.project)
            ?: throw IllegalArgumentException("Project not found: ${request.project}")

        // Save all documents before refactoring
        ProjectUtils.saveAllDocuments()

        val psiFile = ProjectUtils.findPsiFile(project, request.file)
            ?: throw IllegalArgumentException("File not found: ${request.file}")

        val element = PsiUtils.findNamedElementAt(psiFile, request.line, request.column)
            ?: throw IllegalArgumentException("No renamable element found at line ${request.line}, column ${request.column}")

        if (request.preview) {
            return previewRename(project, element, request.newName)
        }

        return performRename(project, element, request)
    }

    private fun previewRename(project: Project, element: PsiNamedElement, newName: String): RenameResponse {
        val changes = mutableListOf<FileChange>()
        val filesModified = mutableSetOf<String>()

        ApplicationManager.getApplication().runReadAction {
            val oldName = element.name ?: ""

            // Add the definition itself
            val defFile = element.containingFile?.virtualFile?.path ?: ""
            filesModified.add(defFile)
            changes.add(
                FileChange(
                    file = defFile,
                    line = PsiUtils.getLineNumber(element),
                    oldText = oldName,
                    newText = newName
                )
            )

            // Find all references
            val references = ReferencesSearch.search(element, GlobalSearchScope.projectScope(project))
            references.forEach { ref ->
                val refElement = ref.element
                val refFile = refElement.containingFile?.virtualFile?.path ?: return@forEach
                filesModified.add(refFile)
                changes.add(
                    FileChange(
                        file = refFile,
                        line = PsiUtils.getLineNumber(refElement),
                        oldText = oldName,
                        newText = newName
                    )
                )
            }
        }

        return RenameResponse(
            success = true,
            changes = changes,
            filesModified = filesModified.size,
            usagesUpdated = changes.size
        )
    }

    private fun performRename(project: Project, element: PsiNamedElement, request: RenameRequest): RenameResponse {
        val changes = mutableListOf<FileChange>()
        val filesModified = mutableSetOf<String>()
        val oldName = ApplicationManager.getApplication().runReadAction<String> {
            element.name ?: ""
        }

        // Collect references before rename
        ApplicationManager.getApplication().runReadAction {
            val defFile = element.containingFile?.virtualFile?.path ?: ""
            filesModified.add(defFile)
            changes.add(
                FileChange(
                    file = defFile,
                    line = PsiUtils.getLineNumber(element),
                    oldText = oldName,
                    newText = request.newName
                )
            )

            val references = ReferencesSearch.search(element, GlobalSearchScope.projectScope(project))
            references.forEach { ref ->
                val refElement = ref.element
                val refFile = refElement.containingFile?.virtualFile?.path ?: return@forEach
                filesModified.add(refFile)
                changes.add(
                    FileChange(
                        file = refFile,
                        line = PsiUtils.getLineNumber(refElement),
                        oldText = oldName,
                        newText = request.newName
                    )
                )
            }
        }

        // Perform the actual rename
        ApplicationManager.getApplication().invokeAndWait {
            CommandProcessor.getInstance().executeCommand(project, {
                WriteAction.run<Throwable> {
                    val processor = RenameProcessor(
                        project,
                        element,
                        request.newName,
                        request.searchInComments,
                        request.searchInStrings
                    )
                    processor.run()
                }
            }, "Rename ${oldName} to ${request.newName}", null)
        }

        return RenameResponse(
            success = true,
            changes = changes,
            filesModified = filesModified.size,
            usagesUpdated = changes.size
        )
    }
}

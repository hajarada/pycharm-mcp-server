package com.github.pycharm.refactoring.refactoring

import com.github.pycharm.refactoring.server.models.SafeDeleteRequest
import com.github.pycharm.refactoring.server.models.SafeDeleteResponse
import com.github.pycharm.refactoring.server.models.UsageInfo
import com.github.pycharm.refactoring.util.ProjectUtils
import com.github.pycharm.refactoring.util.PsiUtils
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.safeDelete.SafeDeleteProcessor

class SafeDeleteService {

    fun safeDelete(request: SafeDeleteRequest): SafeDeleteResponse {
        val project = ProjectUtils.findProjectByPath(request.project)
            ?: throw IllegalArgumentException("Project not found: ${request.project}")

        ProjectUtils.saveAllDocuments()

        val psiFile = ProjectUtils.findPsiFile(project, request.file)
            ?: throw IllegalArgumentException("File not found: ${request.file}")

        val element = PsiUtils.findNamedElementAt(psiFile, request.line, request.column)
            ?: throw IllegalArgumentException("No deletable element found at line ${request.line}, column ${request.column}")

        // Find usages if requested
        if (request.searchForUsages) {
            val usages = findUsages(project, element)
            if (usages.isNotEmpty()) {
                return SafeDeleteResponse(
                    success = true,
                    deleted = false,
                    usagesFound = usages.size,
                    usages = usages
                )
            }
        }

        // No usages found, safe to delete
        return performDelete(project, element)
    }

    private fun findUsages(project: com.intellij.openapi.project.Project, element: PsiNamedElement): List<UsageInfo> {
        val usages = mutableListOf<UsageInfo>()

        ApplicationManager.getApplication().runReadAction {
            val references = ReferencesSearch.search(element, GlobalSearchScope.projectScope(project))
            references.forEach { ref ->
                val refElement = ref.element
                val refFile = refElement.containingFile?.virtualFile?.path ?: return@forEach

                usages.add(
                    UsageInfo(
                        file = refFile,
                        line = PsiUtils.getLineNumber(refElement),
                        column = PsiUtils.getColumnNumber(refElement),
                        text = PsiUtils.getSurroundingText(refElement, 0),
                        isWriteAccess = isWriteAccess(refElement)
                    )
                )
            }
        }

        return usages
    }

    private fun isWriteAccess(element: PsiElement): Boolean {
        return ApplicationManager.getApplication().runReadAction<Boolean> {
            // Check if this is an assignment target
            val parent = element.parent
            when {
                parent is com.jetbrains.python.psi.PyAssignmentStatement -> {
                    parent.targets.any { it == element || it.textRange.contains(element.textRange) }
                }
                parent is com.jetbrains.python.psi.PyAugAssignmentStatement -> true
                else -> false
            }
        }
    }

    private fun performDelete(project: com.intellij.openapi.project.Project, element: PsiNamedElement): SafeDeleteResponse {
        ApplicationManager.getApplication().invokeAndWait {
            CommandProcessor.getInstance().executeCommand(project, {
                WriteAction.run<Throwable> {
                    SafeDeleteProcessor.createInstance(
                        project,
                        null,  // callback
                        arrayOf(element),
                        true,  // search in comments
                        true,  // search in strings
                        true   // search in non-code
                    ).run()
                }
            }, "Safe delete ${element.name}", null)
        }

        return SafeDeleteResponse(
            success = true,
            deleted = true,
            usagesFound = 0,
            usages = null
        )
    }
}

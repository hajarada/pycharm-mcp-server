package com.github.pycharm.refactoring.refactoring

import com.github.pycharm.refactoring.server.models.FindUsagesRequest
import com.github.pycharm.refactoring.server.models.FindUsagesResponse
import com.github.pycharm.refactoring.server.models.UsageInfo
import com.github.pycharm.refactoring.util.ProjectUtils
import com.github.pycharm.refactoring.util.PsiUtils
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch

class FindUsagesService {

    fun findUsages(request: FindUsagesRequest): FindUsagesResponse {
        val project = ProjectUtils.findProjectByPath(request.project)
            ?: throw IllegalArgumentException("Project not found: ${request.project}")

        val psiFile = ProjectUtils.findPsiFile(project, request.file)
            ?: throw IllegalArgumentException("File not found: ${request.file}")

        val element = PsiUtils.findNamedElementAt(psiFile, request.line, request.column)
            ?: throw IllegalArgumentException("No symbol found at line ${request.line}, column ${request.column}")

        return findAllUsages(project, element)
    }

    private fun findAllUsages(project: com.intellij.openapi.project.Project, element: PsiNamedElement): FindUsagesResponse {
        val usages = mutableListOf<UsageInfo>()

        val symbolName = ApplicationManager.getApplication().runReadAction<String> {
            element.name ?: "unknown"
        }

        ApplicationManager.getApplication().runReadAction {
            // Add the definition itself
            val defFile = element.containingFile?.virtualFile?.path ?: ""
            usages.add(
                UsageInfo(
                    file = defFile,
                    line = PsiUtils.getLineNumber(element),
                    column = PsiUtils.getColumnNumber(element),
                    text = PsiUtils.getSurroundingText(element, 1),
                    isWriteAccess = true  // Definition is a "write"
                )
            )

            // Find all references
            val references = ReferencesSearch.search(element, GlobalSearchScope.projectScope(project))
            references.forEach { ref ->
                val refElement = ref.element
                val refFile = refElement.containingFile?.virtualFile?.path ?: return@forEach

                usages.add(
                    UsageInfo(
                        file = refFile,
                        line = PsiUtils.getLineNumber(refElement),
                        column = PsiUtils.getColumnNumber(refElement),
                        text = PsiUtils.getSurroundingText(refElement, 1),
                        isWriteAccess = isWriteAccess(refElement)
                    )
                )
            }
        }

        return FindUsagesResponse(
            success = true,
            symbol = symbolName,
            usages = usages,
            totalCount = usages.size
        )
    }

    private fun isWriteAccess(element: PsiElement): Boolean {
        return ApplicationManager.getApplication().runReadAction<Boolean> {
            val parent = element.parent
            when {
                parent is com.jetbrains.python.psi.PyAssignmentStatement -> {
                    parent.targets.any { it == element || it.textRange.contains(element.textRange) }
                }
                parent is com.jetbrains.python.psi.PyAugAssignmentStatement -> true
                parent is com.jetbrains.python.psi.PyForStatement -> {
                    parent.forPart.target == element
                }
                parent is com.jetbrains.python.psi.PyComprehensionElement -> {
                    // Check if element is a loop variable in comprehension
                    true
                }
                else -> false
            }
        }
    }
}

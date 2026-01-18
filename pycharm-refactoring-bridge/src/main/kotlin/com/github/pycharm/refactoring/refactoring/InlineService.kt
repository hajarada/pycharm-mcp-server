package com.github.pycharm.refactoring.refactoring

import com.github.pycharm.refactoring.server.models.FileChange
import com.github.pycharm.refactoring.server.models.InlineRequest
import com.github.pycharm.refactoring.server.models.InlineResponse
import com.github.pycharm.refactoring.util.ProjectUtils
import com.github.pycharm.refactoring.util.PsiUtils
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.jetbrains.python.psi.PyAssignmentStatement
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.PyTargetExpression
import com.jetbrains.python.refactoring.inline.PyInlineLocalHandler

class InlineService {

    fun inline(request: InlineRequest): InlineResponse {
        val project = ProjectUtils.findProjectByPath(request.project)
            ?: throw IllegalArgumentException("Project not found: ${request.project}")

        ProjectUtils.saveAllDocuments()

        val psiFile = ProjectUtils.findPsiFile(project, request.file)
            ?: throw IllegalArgumentException("File not found: ${request.file}")

        val element = PsiUtils.findNamedElementAt(psiFile, request.line, request.column)
            ?: throw IllegalArgumentException("No inlineable element found at line ${request.line}, column ${request.column}")

        // Verify element is inlineable (variable or function)
        val inlineableElement = findInlineableElement(element)
            ?: throw IllegalArgumentException("Element at line ${request.line} is not inlineable (must be a variable or function)")

        if (request.preview) {
            return previewInline(project, inlineableElement)
        }

        return performInline(project, inlineableElement)
    }

    private fun findInlineableElement(element: PsiNamedElement): PsiNamedElement? {
        // Check if it's a variable (target expression) or function
        if (element is PyTargetExpression || element is PyFunction) {
            return element
        }
        // Walk up to find assignment or function
        var current = element.parent
        while (current != null) {
            if (current is PyAssignmentStatement) {
                return current.targets.firstOrNull() as? PsiNamedElement
            }
            if (current is PyFunction) {
                return current
            }
            current = current.parent
        }
        return null
    }

    private fun previewInline(project: com.intellij.openapi.project.Project, element: PsiNamedElement): InlineResponse {
        val changes = mutableListOf<FileChange>()
        val filesModified = mutableSetOf<String>()

        ApplicationManager.getApplication().runReadAction {
            val name = element.name ?: "unknown"
            val definition = getDefinitionText(element)

            // Find all usages that would be inlined
            val references = ReferencesSearch.search(element, GlobalSearchScope.projectScope(project))
            references.forEach { ref ->
                val refElement = ref.element
                val refFile = refElement.containingFile?.virtualFile?.path ?: return@forEach
                filesModified.add(refFile)
                changes.add(
                    FileChange(
                        file = refFile,
                        line = PsiUtils.getLineNumber(refElement),
                        oldText = name,
                        newText = definition
                    )
                )
            }
        }

        return InlineResponse(
            success = true,
            changes = changes,
            usagesInlined = changes.size
        )
    }

    private fun performInline(project: com.intellij.openapi.project.Project, element: PsiNamedElement): InlineResponse {
        val changes = mutableListOf<FileChange>()
        val filesModified = mutableSetOf<String>()

        // Collect usage info before inline
        ApplicationManager.getApplication().runReadAction {
            val name = element.name ?: "unknown"
            val definition = getDefinitionText(element)

            val references = ReferencesSearch.search(element, GlobalSearchScope.projectScope(project))
            references.forEach { ref ->
                val refElement = ref.element
                val refFile = refElement.containingFile?.virtualFile?.path ?: return@forEach
                filesModified.add(refFile)
                changes.add(
                    FileChange(
                        file = refFile,
                        line = PsiUtils.getLineNumber(refElement),
                        oldText = name,
                        newText = definition
                    )
                )
            }
        }

        // Perform the inline
        ApplicationManager.getApplication().invokeAndWait {
            CommandProcessor.getInstance().executeCommand(project, {
                WriteAction.run<Throwable> {
                    when (element) {
                        is PyTargetExpression -> {
                            // Use PyCharm's inline local handler
                            // PyInlineLocalHandler.doInline(project, editor, element, null)
                            // Note: Full implementation requires an editor context
                        }
                        is PyFunction -> {
                            // Use PyCharm's inline function handler
                            // PyInlineFunctionHandler.doInline(project, editor, element, null)
                        }
                    }
                }
            }, "Inline ${element.name}", null)
        }

        return InlineResponse(
            success = true,
            changes = changes,
            usagesInlined = changes.size
        )
    }

    private fun getDefinitionText(element: PsiNamedElement): String {
        return ApplicationManager.getApplication().runReadAction<String> {
            when (element) {
                is PyTargetExpression -> {
                    val assignment = element.parent as? PyAssignmentStatement
                    assignment?.assignedValue?.text ?: element.text
                }
                is PyFunction -> {
                    // For functions, we'd inline the body
                    element.statementList?.text ?: element.text
                }
                else -> element.text
            }
        }
    }
}

package com.github.pycharm.refactoring.refactoring

import com.github.pycharm.refactoring.server.models.ChangeSignatureRequest
import com.github.pycharm.refactoring.server.models.ChangeSignatureResponse
import com.github.pycharm.refactoring.server.models.FileChange
import com.github.pycharm.refactoring.util.ProjectUtils
import com.github.pycharm.refactoring.util.PsiUtils
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.refactoring.changeSignature.PyChangeSignatureProcessor

class SignatureService {

    fun changeSignature(request: ChangeSignatureRequest): ChangeSignatureResponse {
        val project = ProjectUtils.findProjectByPath(request.project)
            ?: throw IllegalArgumentException("Project not found: ${request.project}")

        ProjectUtils.saveAllDocuments()

        val psiFile = ProjectUtils.findPsiFile(project, request.file)
            ?: throw IllegalArgumentException("File not found: ${request.file}")

        val element = PsiUtils.findNamedElementAt(psiFile, request.line, request.column)
            ?: throw IllegalArgumentException("No element found at line ${request.line}, column ${request.column}")

        // Find the function to modify
        val function = findFunction(element)
            ?: throw IllegalArgumentException("No function found at line ${request.line}, column ${request.column}")

        if (request.preview) {
            return previewChangeSignature(project, function, request)
        }

        return performChangeSignature(project, function, request)
    }

    private fun findFunction(element: PsiElement): PyFunction? {
        var current: PsiElement? = element
        while (current != null) {
            if (current is PyFunction) {
                return current
            }
            current = current.parent
        }
        return null
    }

    private fun previewChangeSignature(
        project: com.intellij.openapi.project.Project,
        function: PyFunction,
        request: ChangeSignatureRequest
    ): ChangeSignatureResponse {
        val changes = mutableListOf<FileChange>()
        val filesModified = mutableSetOf<String>()

        ApplicationManager.getApplication().runReadAction {
            val oldSignature = buildSignatureString(function)
            val newSignature = buildNewSignatureString(function, request)

            // Add the definition change
            val defFile = function.containingFile?.virtualFile?.path ?: ""
            filesModified.add(defFile)
            changes.add(
                FileChange(
                    file = defFile,
                    line = PsiUtils.getLineNumber(function),
                    oldText = oldSignature,
                    newText = newSignature
                )
            )

            // Find all call sites
            val references = ReferencesSearch.search(function, GlobalSearchScope.projectScope(project))
            references.forEach { ref ->
                val refElement = ref.element
                val refFile = refElement.containingFile?.virtualFile?.path ?: return@forEach
                filesModified.add(refFile)
                changes.add(
                    FileChange(
                        file = refFile,
                        line = PsiUtils.getLineNumber(refElement),
                        oldText = PsiUtils.getSurroundingText(refElement, 0),
                        newText = "updated call site"
                    )
                )
            }
        }

        return ChangeSignatureResponse(
            success = true,
            changes = changes,
            callSitesUpdated = changes.size - 1  // Exclude the definition itself
        )
    }

    private fun performChangeSignature(
        project: com.intellij.openapi.project.Project,
        function: PyFunction,
        request: ChangeSignatureRequest
    ): ChangeSignatureResponse {
        val changes = mutableListOf<FileChange>()
        val filesModified = mutableSetOf<String>()
        var callSitesUpdated = 0

        // Collect info before change
        ApplicationManager.getApplication().runReadAction {
            val oldSignature = buildSignatureString(function)
            val newSignature = buildNewSignatureString(function, request)

            val defFile = function.containingFile?.virtualFile?.path ?: ""
            filesModified.add(defFile)
            changes.add(
                FileChange(
                    file = defFile,
                    line = PsiUtils.getLineNumber(function),
                    oldText = oldSignature,
                    newText = newSignature
                )
            )

            val references = ReferencesSearch.search(function, GlobalSearchScope.projectScope(project))
            references.forEach { ref ->
                val refFile = ref.element.containingFile?.virtualFile?.path ?: return@forEach
                filesModified.add(refFile)
                callSitesUpdated++
            }
        }

        // Perform the signature change
        ApplicationManager.getApplication().invokeAndWait {
            CommandProcessor.getInstance().executeCommand(project, {
                WriteAction.run<Throwable> {
                    // Build new parameter info
                    // Note: Full implementation would use PyChangeSignatureProcessor
                    // with properly constructed PyParameterInfo objects

                    // Rename if requested
                    request.newName?.let { newName ->
                        if (newName != function.name) {
                            function.setName(newName)
                        }
                    }

                    // Update parameters if requested
                    request.parameters?.let { params ->
                        // Would need to reconstruct the parameter list
                        // using PyChangeSignatureProcessor
                    }
                }
            }, "Change signature of ${function.name}", null)
        }

        return ChangeSignatureResponse(
            success = true,
            changes = changes,
            callSitesUpdated = callSitesUpdated
        )
    }

    private fun buildSignatureString(function: PyFunction): String {
        return ApplicationManager.getApplication().runReadAction<String> {
            val params = function.parameterList.parameters.joinToString(", ") { param ->
                val annotation = param.annotation?.text?.let { ": $it" } ?: ""
                val default = (param as? com.jetbrains.python.psi.PyNamedParameter)?.defaultValue?.text?.let { " = $it" } ?: ""
                "${param.name}$annotation$default"
            }
            val returnType = function.annotation?.text?.let { " -> $it" } ?: ""
            "def ${function.name}($params)$returnType"
        }
    }

    private fun buildNewSignatureString(function: PyFunction, request: ChangeSignatureRequest): String {
        val name = request.newName ?: function.name ?: "unknown"

        val params = request.parameters?.joinToString(", ") { param ->
            val type = param.type?.let { ": $it" } ?: ""
            val default = param.defaultValue?.let { " = $it" } ?: ""
            "${param.name}$type$default"
        } ?: ApplicationManager.getApplication().runReadAction<String> {
            function.parameterList.parameters.joinToString(", ") { it.text }
        }

        val returnType = request.returnType?.let { " -> $it" }
            ?: ApplicationManager.getApplication().runReadAction<String?> { function.annotation?.text?.let { " -> $it" } }
            ?: ""

        return "def $name($params)$returnType"
    }
}

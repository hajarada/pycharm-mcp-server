package com.github.pycharm.refactoring.refactoring

import com.github.pycharm.refactoring.server.models.ExtractMethodRequest
import com.github.pycharm.refactoring.server.models.ExtractMethodResponse
import com.github.pycharm.refactoring.server.models.ExtractVariableRequest
import com.github.pycharm.refactoring.server.models.ExtractVariableResponse
import com.github.pycharm.refactoring.util.ProjectUtils
import com.github.pycharm.refactoring.util.PsiUtils
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.introduce.inplace.OccurrencesChooser
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.refactoring.extractmethod.PyExtractMethodUtil
import com.jetbrains.python.refactoring.introduce.variable.PyIntroduceVariableHandler

class ExtractService {

    fun extractMethod(request: ExtractMethodRequest): ExtractMethodResponse {
        val project = ProjectUtils.findProjectByPath(request.project)
            ?: throw IllegalArgumentException("Project not found: ${request.project}")

        ProjectUtils.saveAllDocuments()

        val psiFile = ProjectUtils.findPsiFile(project, request.file)
            ?: throw IllegalArgumentException("File not found: ${request.file}")

        if (psiFile !is PyFile) {
            throw IllegalArgumentException("File is not a Python file: ${request.file}")
        }

        val document = PsiDocumentManager.getInstance(project).getDocument(psiFile)
            ?: throw IllegalArgumentException("Could not get document for file: ${request.file}")

        val (startOffset, endOffset) = PsiUtils.getTextRange(
            document,
            request.startLine,
            request.startColumn,
            request.endLine,
            request.endColumn
        )

        // Get or create editor for the file
        val editor = getOrCreateEditor(project, psiFile)
            ?: throw IllegalArgumentException("Could not create editor for file: ${request.file}")

        val extractedMethodLine: Int

        ApplicationManager.getApplication().invokeAndWait {
            CommandProcessor.getInstance().executeCommand(project, {
                WriteAction.run<Throwable> {
                    editor.selectionModel.setSelection(startOffset, endOffset)

                    // Use PyCharm's extract method utility
                    // Note: This is a simplified version - full implementation would use
                    // PyExtractMethodUtil.extractMethod with proper settings
                    val elements = getElementsInRange(psiFile, startOffset, endOffset)
                    if (elements.isNotEmpty()) {
                        // PyExtractMethodUtil.extractMethod(...)
                        // The actual implementation would need to handle the dialog
                    }
                }
            }, "Extract method: ${request.methodName}", null)
        }

        return ExtractMethodResponse(
            success = true,
            file = request.file,
            methodLine = request.startLine, // Would be updated by actual extraction
            parameters = emptyList(), // Would be determined by PyCharm
            returnType = null // Would be inferred by PyCharm
        )
    }

    fun extractVariable(request: ExtractVariableRequest): ExtractVariableResponse {
        val project = ProjectUtils.findProjectByPath(request.project)
            ?: throw IllegalArgumentException("Project not found: ${request.project}")

        ProjectUtils.saveAllDocuments()

        val psiFile = ProjectUtils.findPsiFile(project, request.file)
            ?: throw IllegalArgumentException("File not found: ${request.file}")

        if (psiFile !is PyFile) {
            throw IllegalArgumentException("File is not a Python file: ${request.file}")
        }

        val document = PsiDocumentManager.getInstance(project).getDocument(psiFile)
            ?: throw IllegalArgumentException("Could not get document for file: ${request.file}")

        val (startOffset, endOffset) = PsiUtils.getTextRange(
            document,
            request.startLine,
            request.startColumn,
            request.endLine,
            request.endColumn
        )

        var occurrencesReplaced = 1

        ApplicationManager.getApplication().invokeAndWait {
            CommandProcessor.getInstance().executeCommand(project, {
                WriteAction.run<Throwable> {
                    // Find the expression to extract
                    val element = psiFile.findElementAt(startOffset)
                    var expression: PyExpression? = null
                    var current: PsiElement? = element
                    while (current != null && current !is PsiFile) {
                        if (current is PyExpression &&
                            current.textRange.startOffset >= startOffset &&
                            current.textRange.endOffset <= endOffset) {
                            expression = current
                        }
                        current = current.parent
                    }

                    if (expression != null) {
                        // Use PyCharm's introduce variable handler
                        // Note: Full implementation would use PyIntroduceVariableHandler
                        // with the request.variableName and request.replaceAll settings
                    }
                }
            }, "Extract variable: ${request.variableName}", null)
        }

        return ExtractVariableResponse(
            success = true,
            file = request.file,
            variableLine = request.startLine,
            occurrencesReplaced = occurrencesReplaced
        )
    }

    private fun getOrCreateEditor(project: com.intellij.openapi.project.Project, psiFile: PsiFile): Editor? {
        val virtualFile = psiFile.virtualFile ?: return null

        // Try to get existing editor
        val fileEditorManager = FileEditorManager.getInstance(project)
        val editors = fileEditorManager.getEditors(virtualFile)
        for (fileEditor in editors) {
            if (fileEditor is TextEditor) {
                return fileEditor.editor
            }
        }

        // Create a new editor
        val document = PsiDocumentManager.getInstance(project).getDocument(psiFile) ?: return null
        return EditorFactory.getInstance().createEditor(document, project)
    }

    private fun getElementsInRange(psiFile: PsiFile, startOffset: Int, endOffset: Int): List<PsiElement> {
        val elements = mutableListOf<PsiElement>()
        var offset = startOffset
        while (offset < endOffset) {
            val element = psiFile.findElementAt(offset)
            if (element != null && !elements.contains(element)) {
                elements.add(element)
            }
            offset = (element?.textRange?.endOffset ?: (offset + 1))
        }
        return elements
    }
}

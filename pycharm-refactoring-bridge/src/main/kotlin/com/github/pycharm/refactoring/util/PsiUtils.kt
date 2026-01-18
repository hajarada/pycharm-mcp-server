package com.github.pycharm.refactoring.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.util.PsiTreeUtil

object PsiUtils {

    /**
     * Find the PSI element at a given line and column position.
     * Line and column are 1-indexed (as typically used in editors).
     */
    fun findElementAt(psiFile: PsiFile, line: Int, column: Int): PsiElement? {
        return ApplicationManager.getApplication().runReadAction<PsiElement?> {
            val document = PsiDocumentManager.getInstance(psiFile.project).getDocument(psiFile)
                ?: return@runReadAction null

            // Convert 1-indexed line/column to 0-indexed offset
            val lineStartOffset = document.getLineStartOffset(line - 1)
            val offset = lineStartOffset + (column - 1)

            if (offset < 0 || offset >= document.textLength) {
                return@runReadAction null
            }

            psiFile.findElementAt(offset)
        }
    }

    /**
     * Find the nearest named element (variable, function, class, etc.) at or around a position.
     */
    fun findNamedElementAt(psiFile: PsiFile, line: Int, column: Int): PsiNamedElement? {
        val element = findElementAt(psiFile, line, column) ?: return null
        return ApplicationManager.getApplication().runReadAction<PsiNamedElement?> {
            // Walk up the tree to find a named element
            var current: PsiElement? = element
            while (current != null && current !is PsiFile) {
                if (current is PsiNamedElement && current.name != null) {
                    return@runReadAction current
                }
                current = current.parent
            }
            null
        }
    }

    /**
     * Find all elements of a specific type within a PSI element.
     */
    inline fun <reified T : PsiElement> findChildrenOfType(element: PsiElement): Collection<T> {
        return ApplicationManager.getApplication().runReadAction<Collection<T>> {
            PsiTreeUtil.findChildrenOfType(element, T::class.java)
        }
    }

    /**
     * Get the line number (1-indexed) of a PSI element.
     */
    fun getLineNumber(element: PsiElement): Int {
        return ApplicationManager.getApplication().runReadAction<Int> {
            val document = PsiDocumentManager.getInstance(element.project)
                .getDocument(element.containingFile) ?: return@runReadAction 0
            document.getLineNumber(element.textOffset) + 1
        }
    }

    /**
     * Get the column number (1-indexed) of a PSI element.
     */
    fun getColumnNumber(element: PsiElement): Int {
        return ApplicationManager.getApplication().runReadAction<Int> {
            val document = PsiDocumentManager.getInstance(element.project)
                .getDocument(element.containingFile) ?: return@runReadAction 0
            val lineStart = document.getLineStartOffset(document.getLineNumber(element.textOffset))
            element.textOffset - lineStart + 1
        }
    }

    /**
     * Get a text range from line/column positions (1-indexed).
     */
    fun getTextRange(
        document: Document,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int
    ): Pair<Int, Int> {
        val startOffset = document.getLineStartOffset(startLine - 1) + (startColumn - 1)
        val endOffset = document.getLineStartOffset(endLine - 1) + (endColumn - 1)
        return startOffset to endOffset
    }

    /**
     * Get the text surrounding an element for context.
     */
    fun getSurroundingText(element: PsiElement, contextLines: Int = 0): String {
        return ApplicationManager.getApplication().runReadAction<String> {
            if (contextLines == 0) {
                return@runReadAction element.text
            }

            val document = PsiDocumentManager.getInstance(element.project)
                .getDocument(element.containingFile) ?: return@runReadAction element.text

            val lineNumber = document.getLineNumber(element.textOffset)
            val startLine = maxOf(0, lineNumber - contextLines)
            val endLine = minOf(document.lineCount - 1, lineNumber + contextLines)

            val startOffset = document.getLineStartOffset(startLine)
            val endOffset = document.getLineEndOffset(endLine)

            document.getText(com.intellij.openapi.util.TextRange(startOffset, endOffset))
        }
    }
}

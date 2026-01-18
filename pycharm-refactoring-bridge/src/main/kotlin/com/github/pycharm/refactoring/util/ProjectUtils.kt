package com.github.pycharm.refactoring.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import java.io.File

object ProjectUtils {

    /**
     * Get all currently open projects.
     */
    fun getOpenProjects(): List<Project> {
        return ProjectManager.getInstance().openProjects.filter { !it.isDefault }
    }

    /**
     * Find a project by its base path.
     */
    fun findProjectByPath(path: String): Project? {
        val normalizedPath = File(path).canonicalPath
        return getOpenProjects().find { project ->
            project.basePath?.let { File(it).canonicalPath == normalizedPath } == true
        }
    }

    /**
     * Find a virtual file by path, relative to project or absolute.
     */
    fun findVirtualFile(project: Project, filePath: String): VirtualFile? {
        // Try as absolute path first
        var virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath)

        // Try as relative path to project
        if (virtualFile == null && project.basePath != null) {
            val absolutePath = File(project.basePath, filePath).canonicalPath
            virtualFile = LocalFileSystem.getInstance().findFileByPath(absolutePath)
        }

        return virtualFile
    }

    /**
     * Find a PSI file by path.
     */
    fun findPsiFile(project: Project, filePath: String): PsiFile? {
        val virtualFile = findVirtualFile(project, filePath) ?: return null
        return ApplicationManager.getApplication().runReadAction<PsiFile?> {
            PsiManager.getInstance(project).findFile(virtualFile)
        }
    }

    /**
     * Save all documents before refactoring.
     */
    fun saveAllDocuments() {
        ApplicationManager.getApplication().invokeAndWait {
            ApplicationManager.getApplication().runWriteAction {
                com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().saveAllDocuments()
            }
        }
    }

    /**
     * Get the relative path of a file within a project.
     */
    fun getRelativePath(project: Project, file: VirtualFile): String {
        val basePath = project.basePath ?: return file.path
        return if (file.path.startsWith(basePath)) {
            file.path.removePrefix(basePath).removePrefix("/")
        } else {
            file.path
        }
    }
}

package com.github.pycharm.refactoring.server

import com.github.pycharm.refactoring.refactoring.*
import com.github.pycharm.refactoring.server.models.*
import com.github.pycharm.refactoring.settings.RefactoringBridgeSettings
import com.github.pycharm.refactoring.util.ProjectUtils
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

class RefactoringController {

    private val renameService = RenameService()
    private val moveService = MoveService()
    private val extractService = ExtractService()
    private val inlineService = InlineService()
    private val safeDeleteService = SafeDeleteService()
    private val signatureService = SignatureService()
    private val findUsagesService = FindUsagesService()

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun configureRoutes(routing: Routing) {
        routing.apply {
            // Health check
            get("/health") {
                val projects = ProjectUtils.getOpenProjects()
                call.respond(
                    HealthResponse(
                        status = "ok",
                        version = "0.1.0",
                        projectsOpen = projects.size
                    )
                )
            }

            // List projects
            get("/projects") {
                val projects = ProjectUtils.getOpenProjects().map { project ->
                    ProjectInfo(
                        name = project.name,
                        path = project.basePath ?: "",
                        isOpen = true,
                        isDefault = project.isDefault
                    )
                }
                call.respond(ProjectListResponse(success = true, projects = projects))
            }

            // Rename
            post("/refactor/rename") {
                handleRefactoring(call) {
                    val request = call.receive<RenameRequest>()
                    validateProject(request.project)
                    renameService.rename(request)
                }
            }

            // Move
            post("/refactor/move") {
                handleRefactoring(call) {
                    val request = call.receive<MoveRequest>()
                    validateProject(request.project)
                    moveService.move(request)
                }
            }

            // Extract Method
            post("/refactor/extract-method") {
                handleRefactoring(call) {
                    val request = call.receive<ExtractMethodRequest>()
                    validateProject(request.project)
                    extractService.extractMethod(request)
                }
            }

            // Extract Variable
            post("/refactor/extract-variable") {
                handleRefactoring(call) {
                    val request = call.receive<ExtractVariableRequest>()
                    validateProject(request.project)
                    extractService.extractVariable(request)
                }
            }

            // Inline
            post("/refactor/inline") {
                handleRefactoring(call) {
                    val request = call.receive<InlineRequest>()
                    validateProject(request.project)
                    inlineService.inline(request)
                }
            }

            // Change Signature
            post("/refactor/change-signature") {
                handleRefactoring(call) {
                    val request = call.receive<ChangeSignatureRequest>()
                    validateProject(request.project)
                    signatureService.changeSignature(request)
                }
            }

            // Safe Delete
            post("/refactor/safe-delete") {
                handleRefactoring(call) {
                    val request = call.receive<SafeDeleteRequest>()
                    validateProject(request.project)
                    safeDeleteService.safeDelete(request)
                }
            }

            // Find Usages
            post("/find/usages") {
                handleRefactoring(call) {
                    val request = call.receive<FindUsagesRequest>()
                    validateProject(request.project)
                    findUsagesService.findUsages(request)
                }
            }
        }
    }

    private suspend inline fun <reified T : Any> handleRefactoring(
        call: ApplicationCall,
        crossinline handler: suspend () -> T
    ) {
        try {
            // Check auth token if configured
            val settings = RefactoringBridgeSettings.getInstance()
            if (settings.authToken.isNotEmpty()) {
                val authHeader = call.request.header("Authorization")
                val expectedToken = "Bearer ${settings.authToken}"
                if (authHeader != expectedToken) {
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        ErrorResponse(error = "Unauthorized", details = "Invalid or missing auth token")
                    )
                    return
                }
            }

            val result = handler()
            call.respond(result)
        } catch (e: IllegalArgumentException) {
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(error = "Bad Request", details = e.message)
            )
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(error = "Internal Server Error", details = e.message ?: e.toString())
            )
        }
    }

    private fun validateProject(projectPath: String) {
        val settings = RefactoringBridgeSettings.getInstance()

        // If allowlist is configured, check it
        if (settings.allowedProjectPaths.isNotEmpty()) {
            val normalizedPath = java.io.File(projectPath).canonicalPath
            val allowed = settings.allowedProjectPaths.any { allowedPath ->
                normalizedPath.startsWith(java.io.File(allowedPath).canonicalPath)
            }
            if (!allowed) {
                throw IllegalArgumentException("Project not in allowed list: $projectPath")
            }
        }

        // Verify project is open
        if (ProjectUtils.findProjectByPath(projectPath) == null) {
            throw IllegalArgumentException("Project not open in PyCharm: $projectPath")
        }
    }
}

package com.github.pycharm.refactoring.server

import com.github.pycharm.refactoring.settings.RefactoringBridgeSettings
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class HttpServer : Disposable {

    private val logger = Logger.getInstance(HttpServer::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var server: ApplicationEngine? = null
    private val controller = RefactoringController()

    fun start() {
        val settings = RefactoringBridgeSettings.getInstance()

        if (!settings.enabled) {
            logger.info("Refactoring Bridge server is disabled")
            return
        }

        scope.launch {
            try {
                server = embeddedServer(Netty, port = settings.port, host = "127.0.0.1") {
                    configureServer()
                }.start(wait = false)

                logger.info("Refactoring Bridge server started on port ${settings.port}")
            } catch (e: Exception) {
                logger.error("Failed to start Refactoring Bridge server", e)
            }
        }
    }

    fun stop() {
        server?.stop(1000, 2000)
        server = null
        logger.info("Refactoring Bridge server stopped")
    }

    fun restart() {
        stop()
        start()
    }

    override fun dispose() {
        stop()
    }

    private fun Application.configureServer() {
        // JSON serialization
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }

        // CORS for local development
        install(CORS) {
            allowHost("localhost", schemes = listOf("http"))
            allowHost("127.0.0.1", schemes = listOf("http"))
            allowHeader(HttpHeaders.ContentType)
            allowHeader(HttpHeaders.Authorization)
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Post)
            allowMethod(HttpMethod.Options)
        }

        // Configure routes
        routing {
            controller.configureRoutes(this)
        }
    }
}

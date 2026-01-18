package com.github.pycharm.refactoring

import com.github.pycharm.refactoring.server.HttpServer
import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Disposer

/**
 * Main plugin entry point that starts the HTTP server when the IDE launches.
 */
class RefactoringBridgePlugin : AppLifecycleListener, Disposable {

    private val logger = Logger.getInstance(RefactoringBridgePlugin::class.java)
    private var httpServer: HttpServer? = null

    override fun appFrameCreated(commandLineArgs: List<String>) {
        logger.info("Initializing PyCharm Refactoring Bridge")

        httpServer = HttpServer().also {
            Disposer.register(ApplicationManager.getApplication(), it)
            it.start()
        }
    }

    override fun appWillBeClosed(isRestart: Boolean) {
        logger.info("Shutting down PyCharm Refactoring Bridge")
        httpServer?.stop()
    }

    override fun dispose() {
        httpServer?.stop()
        httpServer = null
    }

    companion object {
        @Volatile
        private var instance: RefactoringBridgePlugin? = null

        fun getInstance(): RefactoringBridgePlugin? = instance
    }
}

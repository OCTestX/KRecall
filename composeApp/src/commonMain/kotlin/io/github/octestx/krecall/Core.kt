package io.github.octestx.krecall

import io.github.kotlin.fibonacci.BasicMultiplatformConfigModule
import io.github.kotlin.fibonacci.JVMInitCenter
import io.github.kotlin.fibonacci.JVMUIInitCenter
import io.github.octestx.krecall.plugins.PluginManager
import io.github.octestx.krecall.plugins.basic.IPluginContext
import io.github.octestx.krecall.plugins.impl.PluginContextImpl
import io.github.octestx.krecall.repository.ConfigManager
import io.github.octestx.krecall.repository.FileTree
import io.klogging.noCoLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.dsl.module
import java.io.File
import java.net.ServerSocket
import kotlin.system.exitProcess

object Core {
    private val ologger = noCoLogger<Core>()
    private val ioscope = CoroutineScope(Dispatchers.IO)
    @Volatile
    private var initialized = false
    suspend fun init() {
        if (initialized) return
        val isSingle = checkSingleInstance()
        if (isSingle.not()) {
            ologger.error { "Already run one now!" }
            exitProcess(18)
        }

        val workDir = File(File(System.getProperty("user.dir")), "KRecall").apply {
            mkdirs()
        }
        val config = BasicMultiplatformConfigModule()
        config.configInnerAppDir(workDir)
        startKoin() {
            modules(
                config.asModule(),
                module {
                    single<IPluginContext> { PluginContextImpl() }
                }
            )
        }
        JVMInitCenter.init()
        JVMUIInitCenter.init()

        FileTree.init()
        runBlocking {
            PluginManager.init()
            if (ConfigManager.config.initialized && ConfigManager.config.initPlugin) {
                PluginManager.initAllPlugins()
            }
        }

        initialized = true
        ologger.info { "INITIALIZED" }
    }

    private var socketServer: ServerSocket? = null
    //if current is only one. return true
    fun checkSingleInstance(): Boolean {
        if (socketServer != null) return true
        try {
            socketServer = ServerSocket(19501)//TODO change port
            return true
        } catch (e: Throwable) {
            return false
        }
    }
}
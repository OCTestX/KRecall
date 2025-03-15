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
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.dsl.module
import java.io.File

object Core {
    private val ologger = noCoLogger<Core>()
    private val ioscope = CoroutineScope(Dispatchers.IO)
    @Volatile
    private var initialized = false
    fun init() {
        if (initialized) return
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
            if (ConfigManager.config.initialized) {
                PluginManager.initAllPlugins()
            }
        }

        initialized = true
        ologger.info { "INITIALIZED" }
    }
}
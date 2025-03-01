package io.github.octestx.krecall.plugins.impl.getscreen

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import io.github.octestx.krecall.plugins.basic.AbsGetScreenPlugin
import io.klogging.noCoLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream

class GetScreenByKDESpectaclePlugin: AbsGetScreenPlugin(pluginId = "GetScreenByKDESpectaclePlugin") {
    private val ologger = noCoLogger<GetScreenByKDESpectaclePlugin>()
    override suspend fun supportOutputToStream(): Boolean = false

    override suspend fun getScreen(outputStream: OutputStream) {
        throw UnsupportedOperationException()
    }

    override suspend fun getScreen(outputFileBitItNotExits: File) {
        withContext(Dispatchers.IO) {
            ologger.info { "getScreen" }
            val processBuilder = ProcessBuilder("/usr/bin/spectacle", "-f", "-b", "-n", "-o", outputFileBitItNotExits.absolutePath)
            processBuilder.redirectErrorStream(false)
            ologger.error { processBuilder.toString() }
            val process = processBuilder.start()
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                val err = RuntimeException("Command[${processBuilder.command()}] failed with exit code $exitCode")
                ologger.error(err) { err.message }
                throw err
            }
        }
    }

    override fun loadInner() {
        ologger.info { "Loaded" }
    }

    @Composable
    override fun UI() {
        Text("GetScreenByKDESpectaclePluginUI")
        val scope = rememberCoroutineScope()
        Button(onClick = {
            scope.launch {
                val f = File("f.png")
                getScreen(f)
                ologger.info("Test: ${f.absolutePath}")
            }
        }) {
            Text("Test")
        }
    }

    override fun tryInitInner(): Exception? {
        ologger.info { "TryInit" }
        val processBuilder = ProcessBuilder("/usr/bin/spectacle")
        processBuilder.redirectErrorStream(false)

        val process = processBuilder.start()
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            return RuntimeException("Command failed with exit code $exitCode")
        }
        initialized = true
        return null
    }

    override var initialized: Boolean = false
}
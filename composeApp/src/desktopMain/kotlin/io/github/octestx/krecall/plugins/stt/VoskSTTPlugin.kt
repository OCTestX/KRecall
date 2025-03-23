package io.github.octestx.krecall.plugins.stt

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.painter.Painter
import io.github.kotlin.fibonacci.ui.toast
import io.github.kotlin.fibonacci.ui.utils.ToastModel
import io.github.kotlin.fibonacci.utils.OS
import io.github.octestx.krecall.plugins.basic.AbsSTTPlugin
import io.klogging.noCoLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.ExperimentalResourceApi
import java.io.*
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.TargetDataLine

class VoskSTTPlugin: AbsSTTPlugin(pluginId = "VoskSTTPlugin") {
    override suspend fun recognize(audioData: ByteArray, controller: STTResultController) {
        TODO("Not yet implemented")
    }

    override val supportPlatform: Set<OS.OperatingSystem> = setOf(OS.OperatingSystem.LINUX, OS.OperatingSystem.WIN, OS.OperatingSystem.MACOS, OS.OperatingSystem.MACOS)
    override val supportUI: Boolean = true
    private val ologger = noCoLogger<VoskSTTPlugin>()

    override fun load() {
        ologger.info { "Loaded" }
    }

    private var audioLine: TargetDataLine? = null
    private var format: AudioFormat? = null
    override fun selected() {

    }
    override fun unselected() {}

    @OptIn(ExperimentalResourceApi::class)
    @Composable
    override fun UI() {
        val scope = rememberCoroutineScope()
        var painter: Painter? by remember { mutableStateOf(null) }
        Column {
            Button(onClick = {
                scope.launch {
                    val f = try {
                        val f1 = test()
                        toast.applyShow(ToastModel("Test passed", type = ToastModel.Type.Success))
                        f1
                    } catch (e: Exception) {
                        ologger.error(e) { "Test fail: ${e.message}" }
                        toast.applyShow(ToastModel("Test fail: ${e.message}", type = ToastModel.Type.Success))
                        throw e
                    }
                    ologger.info("Test: ${f.absolutePath}")
                }
            }) {
                Text("Test")
            }
            painter?.let { Image(it, contentDescription = null) }
        }
    }

    private suspend fun test(): File {
        return withContext(Dispatchers.IO) {
            val f = File(pluginDir, "test.wav1")
            start(BufferedOutputStream(f.outputStream()))
            delay(5000)
            pause()
            outputStream?.close()
            if (f.length() < 44 * 1024) {
                throw FileNotFoundException("testFile too small, maybe it not capture")
            }
            WavHeaderUtil.updateHeader(f)
            val newFile = File(f.parentFile, "test.wav")
            f.renameTo(newFile)
            ologger.info("Test: ${newFile.absolutePath}")
            newFile
        }
    }


    override fun tryInitInner(): InitResult {
        ologger.info { "TryInit" }
        val e = runBlocking {
            try {
                test()
                null
            } catch (e: Exception) {
                e
            }
        }
        if (e == null) {
            _initialized.value = true
            return InitResult.Success
        } else {
            return InitResult.Failed(e)
        }
    }

    private val _initialized = MutableStateFlow(false)
    override val initialized: StateFlow<Boolean> = _initialized
}
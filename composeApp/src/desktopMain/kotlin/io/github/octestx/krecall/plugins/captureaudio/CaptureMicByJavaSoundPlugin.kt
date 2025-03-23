package io.github.octestx.krecall.plugins.captureaudio

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import io.github.kotlin.fibonacci.ui.toast
import io.github.kotlin.fibonacci.ui.utils.ToastModel
import io.github.kotlin.fibonacci.utils.OS
import io.github.octestx.krecall.plugins.basic.AbsCaptureAudioPlugin
import io.klogging.noCoLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.ExperimentalResourceApi
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileNotFoundException
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine

class CaptureMicByJavaSoundPlugin: AbsCaptureAudioPlugin(pluginId = "CaptureMicByJavaSoundPlugin") {
    private val scope = CoroutineScope(Dispatchers.IO)
    @Volatile
    private var receiver: AudioDataReceiver? = null
    private var readerJob: Job? = null
    @Volatile
    private var outputStream: BufferedOutputStream? = null
    override fun provideReceiver(receiver: AudioDataReceiver) {
        this.receiver = receiver
    }

    override fun start(outputStream: BufferedOutputStream) {
        // 预先写入 WAV 头（占位）
        WavHeaderUtil.writeHeader(DataOutputStream(outputStream), format)
        ologger.info { "WAV header written: $format" }
        if (audioLine?.isRunning != true) {
            audioLine?.start()
            ologger.info { "Started" }
        }
        this.outputStream = outputStream
    }

    override fun pause() {
        outputStream?.flush()
        if (audioLine?.isRunning == true) {
            audioLine?.stop()
        }
    }

    override val isCapturing: Boolean
        get() = audioLine?.isRunning?:false

    override val supportPlatform: Set<OS.OperatingSystem> = setOf(OS.OperatingSystem.LINUX, OS.OperatingSystem.WIN, OS.OperatingSystem.MACOS, OS.OperatingSystem.MACOS)
    override val supportUI: Boolean = true
    private val ologger = noCoLogger<CaptureMicByJavaSoundPlugin>()

    override fun load() {
        ologger.info { "CaptureMicByJavaSoundPluginLoaded" }
    }

    private var audioLine: TargetDataLine? = null
    // 1. 初始化音频输入流（Java Sound API）
    // 配置音频格式（关键参数必须与硬件兼容）
    private var format: AudioFormat = AudioFormat(
        AudioFormat.Encoding.PCM_SIGNED,
        44100f,     // 采样率
        16,         // 位深
        2,          // 声道数（立体声）
        (16 / 8) * 2, // 帧大小（字节）
        44100f,     // 帧率
        false       // 小端序
    )
    override fun selected() {
        //TODO
        return
        scope.launch {
            val mixers = AudioSystem.getMixerInfo()
            mixers.forEach { mixerInfo ->
                val mixer = AudioSystem.getMixer(mixerInfo)
                val lineInfos = mixer.targetLineInfo
                lineInfos.forEach { lineInfo ->
                    if (lineInfo.lineClass == TargetDataLine::class.java) {
                        println("找到麦克风设备: ${mixerInfo.name}[${mixerInfo.vendor}]")
                    }
                }
            }
            val info = DataLine.Info(TargetDataLine::class.java, format)
            audioLine = AudioSystem.getLine(info) as TargetDataLine
            audioLine?.open(format)

            readerJob = launch {
                reader()
            }
        }
    }
    private suspend fun reader() {
        ologger.info { "Reader running[audioLine=${audioLine?.isRunning}]" }
        withContext(Dispatchers.IO) {
            val buffer = ByteArray(4096)
            while (true) {
                val line = audioLine ?: return@withContext
                val read = line.read(buffer, 0, buffer.size)
                if (read > 0) {
                    receiver?.receive(buffer)
                    outputStream?.write(buffer, 0, read)
                    ologger.info { "FV" }
                } else {
                    delay(10)
                }
            }
        }
    }
    override fun unselected() {
        audioLine?.close()
        audioLine = null
        readerJob?.cancel()
    }

    @OptIn(ExperimentalResourceApi::class)
    @Composable
    override fun UI() {
        val scope = rememberCoroutineScope()
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
        //TODO
        _initialized.value = true
        return InitResult.Success
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
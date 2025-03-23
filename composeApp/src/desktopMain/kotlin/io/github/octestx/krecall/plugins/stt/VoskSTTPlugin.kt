package io.github.octestx.krecall.plugins.stt

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import io.github.kotlin.fibonacci.ui.toast
import io.github.kotlin.fibonacci.ui.utils.ToastModel
import io.github.kotlin.fibonacci.utils.OS
import io.github.kotlin.fibonacci.utils.ojson
import io.github.octestx.krecall.exceptions.ConfigurationNotSavedException
import io.github.octestx.krecall.plugins.basic.AbsSTTPlugin
import io.github.octestx.krecall.plugins.basic.STTResult
import io.github.octestx.krecall.plugins.impl.storage.OTStoragePlugin.StorageConfig
import io.klogging.noCoLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.io.FileNotFoundException

class VoskSTTPlugin: AbsSTTPlugin(pluginId = "VoskSTTPlugin") {
    override suspend fun emit(audioData: ByteArray, bytesRead: Int) {
        // 将音频数据传入识别器
        recognizer?.acceptWaveForm(audioData, bytesRead)
    }

    override suspend fun getResult(): STTResult {
        val resultJson = recognizer?.finalResult ?: return STTResult("")
        val result = ojson.decodeFromString<VoskFinalResult>(resultJson)
        return STTResult(result.text, result.words)
    }

    override val supportPlatform: Set<OS.OperatingSystem> = setOf(OS.OperatingSystem.LINUX, OS.OperatingSystem.WIN, OS.OperatingSystem.MACOS, OS.OperatingSystem.MACOS)
    override val supportUI: Boolean = true
    private val ologger = noCoLogger<VoskSTTPlugin>()
    private val configFile = File(pluginDir, "config.json")
    @Volatile
    private lateinit var config: StorageConfig

    @Serializable
    data class StorageConfig(
        val modelPath: String,
    )

    override fun load() {
        try {
            config = ojson.decodeFromString(configFile.readText())
        } catch (e: Throwable) {
            ologger.warn { "加载配置文件时遇到错误，已复原: ${configFile.absolutePath}" }
            configFile.renameTo(File(configFile.parentFile, "config.json.old"))
            config = StorageConfig(
                modelPath = ""
            )
            configFile.writeText(ojson.encodeToString(config))
        }
        ologger.info { "Loaded" }
    }

    private var recognizer: Recognizer? = null
    override fun selected() {}
    override fun unselected() {}

    @Composable
    override fun UI() {
        val scope = rememberCoroutineScope()
        Column {
            var filePath by remember { mutableStateOf(config.modelPath) }
            TextField(filePath, {
                filePath = it
                configDataChange()
            }, label = {
                Text("Model文件夹存储路径，为空则默认")
            })
            var saveText = "Save"
            Button(onClick = {
                try {
                    val checkedDir = if (filePath.isEmpty()) {
                        true
                    } else {
                        File(filePath).apply { mkdirs() }.exists()
                    }
                    if (checkedDir) {
                        val newConfig = StorageConfig(filePath)
                        configFile.writeText(ojson.encodeToString(newConfig))
                        config = newConfig
                        scope.launch {
                            saveText = "Saved"
                            delay(3000)
                            saveText = "Save"
                        }
                        savedConfig.value = true
                        ologger.info { "Saved" }
                        toast.applyShow("Saved", type = ToastModel.Type.Success)
                    }
                } catch (e: Throwable) {
                    ologger.error(e)
                }
            }, enabled = initialized.value.not()) {
                Text(saveText)
            }
        }
    }

    private suspend fun test() {
        // TODO
//        val bytes = File("/home/octest/Myself/Project/AllFarmwork/Python/CodeGen/KRecall/composeApp/KRecall/output3.wav").readBytes()
//        emit(bytes, bytes.size)
//        val result = getResult()
//        if (result.text.length < 15) {
//            throw RuntimeException("Data so little. Maybe STT not work?")
//        }
    }


    override fun tryInitInner(): InitResult {
        ologger.info { "TryInit" }
        //TODO
        _initialized.value = true
        return InitResult.Success
        if (savedConfig.value.not()) {
            return InitResult.Failed(ConfigurationNotSavedException())
        }
        if (File(config.modelPath).exists().not()) return InitResult.Failed(FileNotFoundException("No exists: ${config.modelPath}"))
        if (File(config.modelPath).canRead().not()) return InitResult.Failed(FileNotFoundException("Can't read: ${config.modelPath}"))
        val e = runBlocking {
            try {
                val model = Model(config.modelPath) // 模型路径
                // 采样率需匹配 TODO
                recognizer = Recognizer(model, 16000f).apply {
                    setWords(true)
                    setPartialWords(true)
                }
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
    private var savedConfig = MutableStateFlow(true)
    private fun configDataChange() {
        savedConfig.value = false
        _initialized.value = false
    }

    private val _initialized = MutableStateFlow(false)
    override val initialized: StateFlow<Boolean> = _initialized
}

private fun main() {
    val model = Model("/home/octest/Myself/Project/AllFarmwork/Python/CodeGen/KRecall/composeApp/KRecall/vosk-model-small-cn-0.22") // 模型路径
    val recognizer = Recognizer(model, 16000f)      // 采样率需匹配 TODO
    val input = File("/home/octest/Myself/Project/AllFarmwork/Python/CodeGen/KRecall/composeApp/KRecall/output3.wav").inputStream()
    val buffer = ByteArray(4096)
    var bytesRead: Int
    runBlocking {
        while (true) {
            bytesRead = input.read(buffer, 0, buffer.size)
            if (bytesRead == -1 || bytesRead == 0) break
            recognizer.acceptWaveForm(buffer, bytesRead)

            // 获取当前部分结果（非阻塞）
            val partialResult = ojson.decodeFromString(VoskPartialResult.serializer(), recognizer.partialResult)
            if (partialResult.text.isNullOrEmpty().not()) {
                println("实时更新: ${partialResult.text}")
            } else print("A")
        }
        val result = ojson.decodeFromString(VoskFinalResult.serializer(), recognizer.result)
        println("ALL: ${result.text}")
    }
}
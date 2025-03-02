package io.github.octestx.krecall.plugins.impl.screenlanguage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.zhipu.oapi.ClientV4
import com.zhipu.oapi.Constants
import com.zhipu.oapi.service.v4.model.ChatCompletionRequest.builder
import com.zhipu.oapi.service.v4.model.ChatMessage
import com.zhipu.oapi.service.v4.model.ChatMessageRole
import com.zhipu.oapi.service.v4.model.ModelApiResponse
import io.github.kotlin.fibonacci.utils.ojson
import io.github.octestx.krecall.exceptions.ConfigurationNotSavedException
import io.github.octestx.krecall.plugins.basic.AbsScreenLanguageConverterPlugin
import io.github.octestx.krecall.plugins.impl.storage.OTStoragePlugin
import io.github.octestx.krecall.plugins.impl.storage.OTStoragePlugin.StorageConfig
import io.klogging.noCoLogger
import io.ktor.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import java.io.File
import java.util.concurrent.TimeUnit


class ScreenLanguageConverterByZhiPuPlugin: AbsScreenLanguageConverterPlugin("ScreenLanguageConverterByKimiPlugin") {
    private val ologger = noCoLogger<OTStoragePlugin>()
    private val configFile = File(pluginDir, "config.json")
    @Volatile
    private lateinit var config: ScreenLanguageConverterByZhiPuPluginConfig

    @Serializable
    data class ScreenLanguageConverterByZhiPuPluginConfig(
        val apiKey: String,
        val model: String,
        val systemMsg: String
    )
    private val defaultSystemMsg = "你是Win11的Recall功能的新后端，我会给你提供一系列电脑截图,请将截图中的内容分析出来，不要使用自然语言，请分成许多个短小的词语,并且进行合理断词,回答要全面，不能使用‘等’，例如分析文件管理器窗口时把里面包含的所有文件的文件名列出来,使用&@&分割不同的词语,例如像这样(示例不要在输出中出现):\"Clash Verge&@&OTStoragePlugin&@&微信&@&文件夹&@&文件资源管理器&@&所有图片&@&gsd.png&@&slk the first.png&@&code.kt\""

    override suspend fun convert(screen: ByteArray): String {
        val imgBase64 = screen.encodeBase64()
        val messages: MutableList<ChatMessage> = ArrayList()
        val contentList: MutableList<Map<String, Any>> = ArrayList()
        contentList.add(mapOf(
            "type" to "text",
            "text" to config.systemMsg
        ))
        contentList.add(mapOf(
            "type" to "image_url",
            "image_url" to mapOf(
                "url" to imgBase64
            )
        ))
        val chatMessage = ChatMessage(ChatMessageRole.USER.value(), contentList)
        messages.add(chatMessage)

        val chatCompletionRequest = builder()
            .model(config.model)
            .stream(false)
            .invokeMethod(Constants.invokeMethod)
            .messages(messages)
            .build()
        ologger.info { "ConvertingData..." }
        val modelApiResponse: ModelApiResponse = client.invokeModelApi(chatCompletionRequest)
        val msg = modelApiResponse.data.choices[0].message.content.toString()
        ologger.info { "ConvertedData: $msg" }
        return msg
    }

    private val client: ClientV4 by lazy {
        ClientV4.Builder(config.apiKey).networkConfig(
            60,
            60,
            60,
            60,
            TimeUnit.SECONDS
        ).build()
    }
    override fun load() {
        try {
            config = ojson.decodeFromString(configFile.readText())
        } catch (e: Throwable) {
            ologger.warn { "加载配置文件时遇到错误，已复原: ${configFile.absolutePath}" }
            configFile.renameTo(File(configFile.parentFile, "config.json.old"))
            config = ScreenLanguageConverterByZhiPuPluginConfig("", "GLM-4V-Flash", defaultSystemMsg)
            configFile.writeText(ojson.encodeToString(config))
        }
        ologger.info { "Loaded" }
    }

    override fun unload() {}
    private var savedConfig = MutableStateFlow(true)
    @Composable
    override fun UI() {
        val scope = rememberCoroutineScope()
        Column {
            if (savedConfig.collectAsState().value.not()) {
                Text("需要保存", modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.tertiary))
            }
            var apiKey by remember { mutableStateOf(config.apiKey) }
            TextField(apiKey, {
                apiKey = it
                savedConfig.value = false
                _initialized.value = false
            }, label = {
                Text("API-Key")
            })
            var model by remember { mutableStateOf(config.model) }
            TextField(model, {
                model = it
                savedConfig.value = false
                _initialized.value = false
            }, label = {
                Text("API-Model")
            })
            var sysMsg by remember { mutableStateOf(config.systemMsg) }
            TextField(sysMsg, {
                sysMsg = it
                savedConfig.value = false
                _initialized.value = false
            }, label = {
                Text("API-SystemMessage")
            })

            var saveText = "Save"
            Button(onClick = {
                try {
                    if (apiKey.isNotEmpty() && model.isNotEmpty() && sysMsg.isNotEmpty()) {
                        val newConfig = ScreenLanguageConverterByZhiPuPluginConfig(apiKey, model, sysMsg)
                        configFile.writeText(ojson.encodeToString(newConfig))
                        config = newConfig
                        scope.launch {
                            saveText = "Saved"
                            delay(3000)
                            saveText = "Save"
                        }
                        savedConfig.value = true
                        ologger.info { "Saved" }
                    }
                } catch (e: Throwable) {
                    ologger.error(e)
                }
            }) {
                Text(saveText)
            }
        }
    }

    override fun tryInitInner(): Exception? {
//        runBlocking { convert(File("/home/octest/Myself/tmp/Screenshot_20250301_234058.png").readBytes()) }
        val confined = config.apiKey.isNotEmpty() && config.model.isNotEmpty()
        if (savedConfig.value.not()) {
            return ConfigurationNotSavedException()
        }
        if (confined.not()) return IllegalArgumentException("需要补全参数")
        ologger.info { "Initialized" }
        _initialized.value = true
        return null
    }

    private val _initialized = MutableStateFlow(false)
    override val initialized: StateFlow<Boolean> = _initialized
}
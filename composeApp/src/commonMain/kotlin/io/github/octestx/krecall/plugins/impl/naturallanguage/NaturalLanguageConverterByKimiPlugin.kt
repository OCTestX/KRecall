package io.github.octestx.krecall.plugins.impl.naturallanguage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
import io.github.octestx.krecall.plugins.basic.AbsNaturalLanguageConverterPlugin
import io.github.octestx.krecall.plugins.impl.storage.OTStoragePlugin
import io.klogging.noCoLogger
import io.ktor.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.io.File
import java.util.concurrent.TimeUnit

class NaturalLanguageConverterByKimiPlugin: AbsNaturalLanguageConverterPlugin("NaturalLanguageConverterByKimiPlugin") {
    private val ologger = noCoLogger<OTStoragePlugin>()
    private val configFile = File(pluginDir, "config.json")
    @Volatile
    private lateinit var config: NaturalLanguageConverterByZhiPuPluginConfig

    @Serializable
    data class NaturalLanguageConverterByZhiPuPluginConfig(
        val apiKey: String,
        val model: String,
        val systemMsg: String
    )
    private val defaultSystemMsg = "你是一个分词ai,将我给你的文字分割多个词语"

    override suspend fun convert(natural: String): String {
        val messages: MutableList<ChatMessage> = ArrayList()
        val contentList: MutableList<Map<String, Any>> = ArrayList()
        val textMap: MutableMap<String, Any> = HashMap()
        textMap["text"] = config.systemMsg
        contentList.add(mapOf(
            "type" to "text",
            "text" to config.systemMsg
        ))
        contentList.add(mapOf(
            "type" to "text",
            "text" to natural
        ))
        val chatMessage = ChatMessage(ChatMessageRole.USER.value(), contentList)
        messages.add(chatMessage)

        val chatCompletionRequest = builder()
            .model(config.model)
            .stream(false)
            .invokeMethod(Constants.invokeMethod)
            .messages(messages)
            .build()
        val modelApiResponse: ModelApiResponse = client.invokeModelApi(chatCompletionRequest)
        val msg = modelApiResponse.msg
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
            config = NaturalLanguageConverterByZhiPuPluginConfig("", "GLM-4-Flash", defaultSystemMsg)
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
                        val newConfig = NaturalLanguageConverterByZhiPuPluginConfig(apiKey, model, sysMsg)
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

    override fun tryInitInner(): InitResult {
//        runBlocking { convert(File("/home/octest/Myself/tmp/Screenshot_20250301_234058.png").readBytes()) }
        val confined = config.apiKey.isNotEmpty() && config.model.isNotEmpty()
        if (savedConfig.value.not()) {
            return InitResult.Failed(ConfigurationNotSavedException())
        }
        if (confined.not()) return InitResult.Failed(IllegalArgumentException("需要补全参数"))
        _initialized.value = true
        return InitResult.Success
    }

    private val _initialized = MutableStateFlow(false)
    override val initialized: StateFlow<Boolean> = _initialized
}
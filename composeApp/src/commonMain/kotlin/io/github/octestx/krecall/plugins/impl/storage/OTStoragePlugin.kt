package io.github.octestx.krecall.plugins.impl.storage

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import io.github.octestx.krecall.plugins.basic.AbsStoragePlugin
import io.klogging.noCoLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileNotFoundException
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Paths

class OTStoragePlugin: AbsStoragePlugin(pluginId = "OTStoragePlugin") {
    private val ologger = noCoLogger<OTStoragePlugin>()
    private val configFile = File(pluginDir, "config.json")
    private val kjson = Json {
        prettyPrint = true
        isLenient = true
    }
    @Volatile
    private lateinit var config: StorageConfig

    @Serializable
    data class StorageConfig(
        val filePath: String?,
        val limitStorage: Long
    )
    override suspend fun requireOutputStream(timestamp: Long): OutputStream = requireFileBitItNotExits(timestamp).apply { createNewFile() }.outputStream()

    override suspend fun requireFileBitItNotExits(timestamp: Long): File {
        val f = File(getScreenDir(), "$timestamp.png")
        if (f.exists()) f.delete()
        return f
    }

    override suspend fun processed(timestamp: Long) {
        withContext(Dispatchers.IO) {
            val store = Files.getFileStore(Paths.get(getScreenDir().absolutePath))
            if (store.usableSpace < config.limitStorage) {
                val list = listTimestampWithNotMark("Deleted").sorted()
                //为了避免持续性清理，一次性清除1.5倍的数据
                val needSpace = (config.limitStorage - store.usableSpace) * 1.5
                var countSpace = 0L
                val countedFiles = mutableListOf<Pair<Long, File>>()
                for (itemTimeStamp in list) {
                    val f = File(getScreenDir(), "$itemTimeStamp.png")
                    countSpace += f.length()
                    countedFiles.add(itemTimeStamp to f)
                    if (countSpace >= needSpace) break
                }
                for (f in countedFiles) {
                    f.second.delete()
                    mark(f.first, "Deleted")
                    ologger.info { "DeleteFile: ${f.second.absolutePath}" }
                }
                ologger.info { "已清理$countSpace Bytes空间" }
            }
        }
        //TODO 图片优化存储
    }

    override suspend fun getScreenData(timestamp: Long): Result<ByteArray> {
        val f = File(getScreenDir(), "$timestamp.png")
        return if (f.exists()) Result.success(f.readBytes())
        else Result.failure(FileNotFoundException(f.absolutePath))
    }

    private fun getScreenDir() = if (config.filePath == null) imageDir else File(config.filePath!!)
    override fun loadInner() {
        ologger.info { "Loaded" }
        try {
            config = kjson.decodeFromString(configFile.readText())
        } catch (e: Throwable) {
            ologger.warn { "加载配置文件时遇到错误，已复原: ${configFile.absolutePath}" }
            configFile.renameTo(File(configFile.parentFile, "config.json.old"))
            config = StorageConfig(
                filePath = null,
                limitStorage = 20L * 1024 * 1024 * 1024
            )
            configFile.writeText(kjson.encodeToString(config))
        }
    }

    @Composable
    override fun UI() {
        val scope = rememberCoroutineScope()
        var filePath by remember { mutableStateOf(config.filePath ?: "") }
        Column {
            TextField(filePath, { filePath = it }, label = {
                Text("文件存储路径，为空则默认")
            })
            var limitStorage by remember { mutableStateOf((config.limitStorage / (1024L * 1024 * 1024)).toString()) }
            val num = limitStorage.toIntOrNull()
            Row {
                TextField(limitStorage, { limitStorage = it }, label = {
                    val text = if (num == null) "只能输入数字!" else "$num GB"
                    Text("存储限制[$text]")
                })
                Text("GB")
            }
            var saveText = "Save"
            Button(onClick = {
                try {
                    if (num != null && File(filePath).exists()) {
                        val newConfig = StorageConfig(filePath, num * (1024L * 1024 * 1024))
                        configFile.writeText(kjson.encodeToString(newConfig))
                        config = newConfig
                        scope.launch {
                            saveText = "Saved"
                            delay(3000)
                            saveText = "Save"
                        }
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
        ologger.info { "TryInit" }
        val processBuilder = ProcessBuilder("spectacle")
        processBuilder.redirectErrorStream(false)

        val process = processBuilder.start()
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            val err = RuntimeException("Command failed with exit code $exitCode")
            return err
        }
        initialized = true
        return null
    }

    override var initialized: Boolean = false
}
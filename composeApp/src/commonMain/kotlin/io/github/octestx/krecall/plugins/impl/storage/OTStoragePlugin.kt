package io.github.octestx.krecall.plugins.impl.storage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import io.github.kotlin.fibonacci.utils.linkFile
import io.github.kotlin.fibonacci.utils.ojson
import io.github.octestx.krecall.exceptions.ConfigurationNotSavedException
import io.github.octestx.krecall.plugins.basic.AbsStoragePlugin
import io.github.octestx.krecall.utils.toKPath
import io.klogging.noCoLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File
import java.io.FileNotFoundException
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Paths

class OTStoragePlugin: AbsStoragePlugin(pluginId = "OTStoragePlugin") {
    private val ologger = noCoLogger<OTStoragePlugin>()
    private val configFile = File(pluginDir, "config.json")
    @Volatile
    private lateinit var config: StorageConfig

    @Serializable
    data class StorageConfig(
        val filePath: String,
        val limitStorage: Long,
        val imageSimilarityLimit: Float,
    )
    override suspend fun requireOutputStream(timestamp: Long): OutputStream = requireFileBitItNotExits(timestamp).apply { createNewFile() }.outputStream()
    private fun getFile(fileTimestamp: Long): File {
        return File(getScreenDir(), "$fileTimestamp.png")
    }

    override suspend fun requireFileBitItNotExits(timestamp: Long): File {
        val f = getFile(timestamp)
        if (f.exists()) f.delete()
        OTStorageDB.addNewRecord(timestamp, timestamp)
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
                    val f = getFile(itemTimeStamp)
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
        val previousFileTimestamp = OTStorageDB.getPreviousData(timestamp)?.fileTimestamp
        if (previousFileTimestamp != null) {
            val currentImg = getScreenDataByFileTimestamp(timestamp).getOrNull()
            val previousImg = getScreenDataByFileTimestamp(previousFileTimestamp).getOrNull()
            if (currentImg != null && previousImg != null) {
                val similarity = ImageUtils.calculateImageSimilarityCV(currentImg, previousImg)
                ologger.info { "图片相似度检测结果: $similarity (${timestamp} vs ${previousFileTimestamp})" }

                if (similarity > config.imageSimilarityLimit) {
                    //TODO 相似度算法残废
//                    // 更新索引并删除当前文件
//                    OTStorageDB.setFileTimestamp(timestamp, previousFileTimestamp)
//                    getFile(timestamp).delete()
                    ologger.info { "相似度${"%.2f".format(similarity*100)}% 超过阈值，已复用历史图片{并没有}" }
                    return
                }
            }
        }
        //TODO 图片优化存储
    }

    override suspend fun getScreenData(timestamp: Long): Result<ByteArray> {
        var fileTimestamp = OTStorageDB.getData(timestamp)?.fileTimestamp
        //TODO以前的数据没有
        if (fileTimestamp == null) {
            fileTimestamp = timestamp
            OTStorageDB.addNewRecord(timestamp, timestamp)
        }
        return getScreenDataByFileTimestamp(fileTimestamp)
    }

    private suspend fun getScreenDataByFileTimestamp(timestamp: Long): Result<ByteArray> {
        val f = File(getScreenDir(), "$timestamp.png")
        return if (f.exists()) Result.success(f.readBytes())
        else Result.failure(FileNotFoundException(f.absolutePath))
    }

    private fun getScreenDir() = if (config.filePath.isEmpty()) imageDir else File(config.filePath)
    override fun load() {
        try {
            config = ojson.decodeFromString(configFile.readText())
        } catch (e: Throwable) {
            ologger.warn { "加载配置文件时遇到错误，已复原: ${configFile.absolutePath}" }
            configFile.renameTo(File(configFile.parentFile, "config.json.old"))
            config = StorageConfig(
                filePath = "",
                limitStorage = 20L * 1024 * 1024 * 1024,
                0.95f,
            )
            configFile.writeText(ojson.encodeToString(config))
        }
        OTStorageDB.init(pluginDir.toKPath().linkFile("data.db"))
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
            var filePath by remember { mutableStateOf(config.filePath ?: "") }
            TextField(filePath, {
                filePath = it
                configDataChange()
            }, label = {
                Text("文件存储路径，为空则默认")
            })
            var limitStorage by remember { mutableStateOf((config.limitStorage / (1024L * 1024 * 1024)).toString()) }
            val num = limitStorage.toIntOrNull()
            Row {
                TextField(limitStorage, {
                    limitStorage = it
                    configDataChange()
                }, label = {
                    val text = if (num == null) "只能输入数字!" else "$num GB"
                    Text("存储限制[$text]")
                })
                Text("GB")
            }
            var imageSimilarityLimit by remember { mutableStateOf(config.imageSimilarityLimit) }
            Text("drop similarity more that: ${imageSimilarityLimit * 100} % Image")
            Slider(imageSimilarityLimit, {
                imageSimilarityLimit = it
                configDataChange()
            })
            var saveText = "Save"
            Button(onClick = {
                try {
                    val checkedDir = if (filePath.isEmpty()) {
                        true
                    } else {
                        File(filePath).apply { mkdirs() }.exists()
                    }
                    if (num != null && checkedDir) {
                        val newConfig = StorageConfig(filePath, num * (1024L * 1024 * 1024), imageSimilarityLimit)
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
        ologger.info { "TryInit" }
        if (savedConfig.value.not()) {
            return InitResult.Failed(ConfigurationNotSavedException())
        }
        if (getScreenDir().canRead().not()) return InitResult.Failed(FileNotFoundException("Can't read: ${getScreenDir().absolutePath}"))
        ologger.info { "Initialized" }
        _initialized.value = true
        return InitResult.Success
    }

    private val _initialized = MutableStateFlow(false)
    override val initialized: StateFlow<Boolean> = _initialized

    private fun configDataChange() {
        savedConfig.value = false
        _initialized.value = false
    }
}
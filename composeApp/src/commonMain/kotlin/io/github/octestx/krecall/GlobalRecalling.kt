package io.github.octestx.krecall

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import io.github.octestx.krecall.plugins.PluginManager
import io.github.octestx.krecall.plugins.basic.AIResult
import io.github.octestx.krecall.plugins.basic.exceptionSerializableOjson
import io.github.octestx.krecall.repository.ConfigManager
import io.github.octestx.krecall.repository.DataDB
import io.github.octestx.krecall.repository.TimeStamp
import io.github.octestx.krecall.utils.ObservableLinkedList
import io.github.octestx.krecall.utils.synchronized
import io.klogging.noCoLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

object GlobalRecalling {
    private val ologger = noCoLogger<GlobalRecalling>()
    private val ioscope = CoroutineScope(Dispatchers.IO)

    val allTimestamp = mutableStateListOf<Long>()
    val collectingScreen = MutableStateFlow(true)
    val collectingDelay = MutableStateFlow(0L)
    val processingData = MutableStateFlow(true)
    val errorTimestamp = mutableStateMapOf<Long, AIResult.Failed<*>>()
    val errorTimestampCount = MutableStateFlow(0)

    //Timestamp
    val processingDataList = ObservableLinkedList<Long>()

    val imageLoadingDispatcher = Dispatchers.IO.limitedParallelism(4)
    private const val MaxCacheSize = 100
    val imageCache: MutableMap<Long, ByteArray?> = object : LinkedHashMap<Long, ByteArray?>(MaxCacheSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, ByteArray?>): Boolean {
            return size > MaxCacheSize
        }
    }.synchronized() // 添加线程安全包装

    private var initialized = false
    fun init() {
        if (initialized) return
        initialized = true
        allTimestamp.addAll(DataDB.listAllData().map { it.timestamp })
        val pairs = DataDB.listNotProcessedData().filter { it.status == 2L }.map { it.timestamp to exceptionSerializableOjson.decodeFromString<AIResult.Failed<String>>(it.error!!) }
        errorTimestamp.putAll(pairs)
        errorTimestampCount.value = errorTimestamp.size
        val collectingScreenJob = ioscope.launch {
            try {
                while (true) {
                    if (collectingScreen.value) {
                        ologger.info { "CollectingScreenJobLoop" }
                        val getScreen = PluginManager.getCaptureScreenPlugin().getOrThrow()
                        val storage = PluginManager.getStoragePlugin().getOrThrow()
                        val timestamp = TimeStamp.current
                        if (getScreen.supportOutputToStream()) {
                            val outputStream =storage.requireImageOutputStream(timestamp)
                            getScreen.getScreen(outputStream)
                        } else {
                            val file = storage.requireImageFileBitItNotExits(timestamp)
                            getScreen.getScreen(file)
                        }
                        DataDB.addNewRecord(timestamp)
                        processingDataList.addLast(timestamp)
                        allTimestamp.add(timestamp)
                    }
                    // refresh collecting screen delay for each 500 ms
                    for (i in 0 until (ConfigManager.config.collectScreenDelay / 50)) {
                        delay(50)
                        collectingDelay.value = (i + 1) * 50
                    }
                    collectingDelay.value = 0
                }
            } catch (e: Exception) {
                ologger.error(e) { "Collecting Fail!" }
            }
        }
        val processingDataJob = ioscope.launch {
            val needProcessData = DataDB.listNotProcessedData()
            for (data in needProcessData) {
                processingDataList.addLast(data.timestamp)
            }
            while (true) {
                if (processingData.value) {
                    ologger.info { "ProcessingDataJobLoop" }
                    val timestamp = processingDataList.pollLastOrNull()
                    try {
                        if (timestamp == null) {
                            delay(1000)
                            continue
                        }
                        val storage = PluginManager.getStoragePlugin().getOrThrow()
                        val captureScreenPlugin = PluginManager.getOCRPlugin().getOrThrow()
                        val screen = storage.getScreenData(timestamp)
                        screen.onSuccess {
                            val data = captureScreenPlugin.recognize(it)
                            if (data is AIResult.Success) {
                                DataDB.appendData(timestamp, data.result)
                                storage.processed(timestamp)
                                DataDB.processed(timestamp)
                            } else if (data is AIResult.Failed<String>) {
                                DataDB.happenError(timestamp, data)
                                errorTimestamp[timestamp] = data
                                errorTimestampCount.value = errorTimestamp.size
                            }
                        }
                        ologger.info { "processed: $timestamp" }
                    } catch (e: Throwable) {
                        ologger.error(e) { "When processing the $timestamp catch a exception: ${e.message}" }
                    }
                }
            }
        }
    }
}
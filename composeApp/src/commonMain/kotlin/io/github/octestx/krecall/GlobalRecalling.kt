package io.github.octestx.krecall

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import io.github.octestx.krecall.plugins.PluginManager
import io.github.octestx.krecall.plugins.basic.AIResult
import io.github.octestx.krecall.plugins.basic.AbsCaptureAudioPlugin
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
    val collectingAudio = MutableStateFlow(true)
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
                        val captureScreen = PluginManager.getCaptureScreenPlugin().getOrThrow()
                        val storage = PluginManager.getStoragePlugin().getOrThrow()
                        val timestamp = TimeStamp.current
                        if (captureScreen.supportOutputToStream()) {
                            val outputStream =storage.requireImageOutputStream(timestamp)
                            captureScreen.getScreen(outputStream)
                        } else {
                            val file = storage.requireImageFileBitItNotExits(timestamp)
                            captureScreen.getScreen(file)
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
        val audioDataReceiver = object : AbsCaptureAudioPlugin.AudioDataReceiver {
            override fun receive(data: ByteArray) {
                //TODO
                ologger.info { "RECEIVE AUDIO DATA: ${data.size}" }
            }
        }
        //TODO capture audio
//        var currentCaptureAudioOutputStream: BufferedOutputStream? = null
//        val collectingAudioJob = ioscope.launch {
//            try {
//                val captureAudio = PluginManager.getCaptureAudioPlugin().getOrThrow()
//                captureAudio.provideReceiver(audioDataReceiver)
//                while (true) {
//                    val leastTimestamp = DataDB.getLeastTimestamp() ?: continue
//                    if (collectingAudio.value && captureAudio.isCapturing.not()) {
//                        ologger.info { "CollectingAudioJobLoop" }
//                        val storage = PluginManager.getStoragePlugin().getOrThrow()
//                        val output = storage.requireAudioOutputStream(leastTimestamp)
//                        val outputStream = BufferedOutputStream(output)
//                        currentCaptureAudioOutputStream = outputStream
//                        captureAudio.start(outputStream)
//                        // TODO change delay time
//                        delay(150)
//                    }
//                }
//            } catch (e: Exception) {
//                ologger.error(e) { "Collecting Fail!" }
//            }
//        }
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
                                //让音频捕获插件暂停换文件, 在collectingAudioJob中, 当它判断插件被暂停了, 会重新启动并携带新文件
                                val captureAudioPlugin = PluginManager.getCaptureAudioPlugin().getOrNull()
//                                if (captureAudioPlugin != null) {
//                                    currentCaptureAudioOutputStream?.close()
//                                    captureAudioPlugin.pause()
//                                }
                                storage.processed(timestamp)
                                DataDB.processed(timestamp)
                                errorTimestamp.remove(timestamp)
                                errorTimestampCount.value = errorTimestamp.size
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
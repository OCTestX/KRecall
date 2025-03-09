package io.github.octestx.krecall

import androidx.compose.runtime.mutableStateListOf
import io.github.octestx.krecall.plugins.PluginManager
import io.github.octestx.krecall.repository.ConfigManager
import io.github.octestx.krecall.repository.DataDB
import io.github.octestx.krecall.repository.TimeStamp
import io.github.octestx.krecall.utils.ObservableLinkedList
import io.klogging.noCoLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import models.sqld.DataItem
import java.util.LinkedList

object GlobalRecalling {
    private val ologger = noCoLogger<GlobalRecalling>()
    private val ioscope = CoroutineScope(Dispatchers.IO)

    val allTimestamp = mutableStateListOf<Long>()
    val collectingScreen = MutableStateFlow(false)
    val processingData = MutableStateFlow(false)

    //Timestamp
    val processingDataList = ObservableLinkedList<Long>()

    private var initialized = false
    fun init() {
        if (initialized) return
        initialized = true
        allTimestamp.addAll(DataDB.listAllData().map { it.timestamp })
        val collectingScreenJob = ioscope.launch {
            while (true) {
                if (collectingScreen.value) {
                    ologger.info { "CollectingScreenJobLoop" }
                    val getScreen = PluginManager.getScreenPlugin().getOrThrow()
                    val storage = PluginManager.getStoragePlugin().getOrThrow()
                    val timestamp = TimeStamp.current
                    if (getScreen.supportOutputToStream()) {
                        val outputStream =storage.requireOutputStream(timestamp)
                        getScreen.getScreen(outputStream)
                    } else {
                        val file = storage.requireFileBitItNotExits(timestamp)
                        getScreen.getScreen(file)
                    }
                    DataDB.addNewRecord(timestamp)
                    processingDataList.addLast(timestamp)
                    allTimestamp.add(timestamp)
                }
                delay(ConfigManager.config.collectScreenDelay)
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
                        val screenLanguageConverterPlugin = PluginManager.getScreenLanguageConverterPlugin().getOrThrow()
                        val screen = storage.getScreenData(timestamp)
                        screen.onSuccess {
                            val data = screenLanguageConverterPlugin.convert(it)
                            DataDB.appendData(timestamp, data)
                            storage.processed(timestamp)
                            DataDB.processed(timestamp)
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
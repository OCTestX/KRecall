package io.github.octestx.krecall.ui

import TimestampRateController
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import io.github.octestx.krecall.GlobalRecalling
import io.github.octestx.krecall.plugins.PluginManager
import io.github.octestx.krecall.repository.DataDB
import io.klogging.noCoLogger
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import ui.core.AbsUIPage
import java.io.ByteArrayInputStream

class HomePage(model: PluginConfigModel): AbsUIPage<Any?, HomePage.HomePageState, HomePage.HomePageAction>(model) {
    private val ologger = noCoLogger<HomePage>()
    @OptIn(ExperimentalResourceApi::class)
    @Composable
    override fun UI(state: HomePageState) {
        Column {
            Row {
                val collectingScreen by GlobalRecalling.collectingScreen.collectAsState()
                Text("CollectingScreen")
                Switch(collectingScreen, { state.action(HomePageAction.ChangeCollectingScreen(!collectingScreen)) })
            }
            Row {
                val processingData by GlobalRecalling.processingData.collectAsState()
                Text("ProcessingData")
                Switch(processingData, { state.action(HomePageAction.ChangeProcessingData(!processingData)) })
            }
            Text("ProcessingData: ${GlobalRecalling.processingDataList.count.collectAsState().value}")
            Column {
                var selectedTimestamp = remember { mutableStateOf(GlobalRecalling.allTimestamp.last()) }
                var theNowMode by remember { mutableStateOf(true) }
                // 添加时间戳控制器
                TimestampRateController(
                    timestamps = GlobalRecalling.allTimestamp,
                    currentTimestamp = selectedTimestamp
                )
                var currentImagePainter: Painter? by remember { mutableStateOf(null) }
                var currentData: String by remember { mutableStateOf("") }
                LaunchedEffect(selectedTimestamp.value) {
                    PluginManager.getStoragePlugin().onSuccess { storagePlugin ->
                        currentData = DataDB.getData(selectedTimestamp.value)?.data_?:""
                        storagePlugin.getScreenData(selectedTimestamp.value).onSuccess {
                            val img = ByteArrayInputStream(it).readAllBytes().decodeToImageBitmap()
                            currentImagePainter = BitmapPainter(img)
                        }
                    }
                }
                Text("Data: $currentData", maxLines = 3)
                currentImagePainter?.apply {
                    Image(
                        painter = this,
                        contentDescription = "Screen"
                    )
                }
            }
        }
    }
    sealed class HomePageAction : AbsUIAction() {
        data class ChangeCollectingScreen(val collectingScreen: Boolean): HomePageAction()
        data class ChangeProcessingData(val processingData: Boolean): HomePageAction()
    }
    data class HomePageState(
        val action: (HomePageAction) -> Unit
    ): AbsUIState<HomePageAction>()

    class PluginConfigModel: AbsUIModel<Any?, HomePageState, HomePageAction>() {
        val ologger = noCoLogger<PluginConfigModel>()

        init {
            GlobalRecalling.init()
        }

        @Composable
        override fun CreateState(params: Any?): HomePageState {
            return HomePageState() {
                actionExecute(params, it)
            }
        }
        override fun actionExecute(params: Any?, action: HomePageAction) {
            when(action) {
                is HomePageAction.ChangeCollectingScreen -> GlobalRecalling.collectingScreen.value = action.collectingScreen
                is HomePageAction.ChangeProcessingData -> GlobalRecalling.processingData.value = action.processingData
            }
        }
    }
}
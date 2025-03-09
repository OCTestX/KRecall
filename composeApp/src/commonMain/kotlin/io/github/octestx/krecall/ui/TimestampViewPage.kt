package io.github.octestx.krecall.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import compose.icons.TablerIcons
import compose.icons.tablericons.ArrowBack
import io.github.octestx.krecall.GlobalRecalling
import io.github.octestx.krecall.model.ImageState
import io.github.octestx.krecall.plugins.PluginManager
import io.klogging.noCoLogger
import kotlinx.coroutines.withContext
import models.sqld.DataItem
import ui.core.AbsUIPage

class TimestampViewPage(private val model: TimestampViewPageModel): AbsUIPage<Any?, TimestampViewPage.TimestampViewPageState, TimestampViewPage.TimestampViewPageAction>(model) {
    private val ologger = noCoLogger<TimestampViewPage>()
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun UI(state: TimestampViewPageState) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            TopAppBar(title = {
                Text(text = "Timestamp: ${state.dataItem.timestamp}")
            }, navigationIcon = {
                IconButton(onClick = {
                    state.action(TimestampViewPageAction.GoBack)
                }) {
                    Icon(TablerIcons.ArrowBack, null)
                }
            })
            when (state.imageState) {
                ImageState.Error -> {
                    Text("ERROR!")
                }
                ImageState.Loading -> {
                    CircularProgressIndicator()
                }
                is ImageState.Success -> {
                    AsyncImage(state.imageState.bytes, null, contentScale = ContentScale.Crop)
                }
            }
            BasicText(
                text = highlightText(
                    text = state.dataItem.data_ ?: "NULL",
                    highlight = model.highlightSearchStr
                ),
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
    private fun highlightText(text: String, highlight: String?): AnnotatedString {
        return buildAnnotatedString {
            if (highlight.isNullOrEmpty()) {
                append(text)
                return@buildAnnotatedString
            }

            var start = 0
            while (true) {
                val index = text.indexOf(highlight, start, ignoreCase = false)
                if (index == -1) {
                    append(text.substring(start))
                    break
                }

                // 添加非高亮部分
                append(text.substring(start, index))

                // 添加高亮部分
                withStyle(
                    style = SpanStyle(
                        background = Color.Yellow.copy(alpha = 0.4f)
                    )
                ) {
                    append(text.substring(index, index + highlight.length))
                }

                start = index + highlight.length
            }
        }
    }
    sealed class TimestampViewPageAction : AbsUIAction() {
        data object GoBack: TimestampViewPageAction()
    }
    data class TimestampViewPageState(
        val dataItem: DataItem,
        val imageState: ImageState,
        val action: (TimestampViewPageAction) -> Unit
    ): AbsUIState<TimestampViewPageAction>()

    class TimestampViewPageModel(private val dataItem: DataItem, val highlightSearchStr: String? = null, private val goBack: () -> Unit): AbsUIModel<Any?, TimestampViewPageState, TimestampViewPageAction>() {
        val ologger = noCoLogger<TimestampViewPageModel>()

        private var imgState: ImageState by  mutableStateOf(ImageState.Loading)

        @Composable
        override fun CreateState(params: Any?): TimestampViewPageState {
            // ✅ 优化后的加载逻辑
            LaunchedEffect(Unit) {
                if (GlobalRecalling.imageCache.containsKey(dataItem.timestamp)) {
                    imgState = ImageState.Success(GlobalRecalling.imageCache[dataItem.timestamp]!!)
                    return@LaunchedEffect
                }

                imgState = try {
                    withContext(GlobalRecalling.imageLoadingDispatcher) {
                        val bytes = GlobalRecalling.imageCache.getOrPut(dataItem.timestamp) {
                            PluginManager.getStoragePlugin().getOrNull()
                                ?.getScreenData(dataItem.timestamp)
                                ?.getOrNull()
                        }
                        if (bytes == null) {
                            ImageState.Error
                        } else {
                            ImageState.Success(bytes)
                        }
                    }
                } catch (e: Exception) {
                    ImageState.Error
                }
            }
            return TimestampViewPageState(dataItem, imgState) {
                actionExecute(params, it)
            }
        }
        override fun actionExecute(params: Any?, action: TimestampViewPageAction) {
            when(action) {
                TimestampViewPageAction.GoBack -> goBack()
            }
        }
    }
}
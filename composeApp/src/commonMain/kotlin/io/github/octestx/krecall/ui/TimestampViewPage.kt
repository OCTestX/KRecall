package io.github.octestx.krecall.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import compose.icons.TablerIcons
import compose.icons.tablericons.ArrowBack
import io.github.octestx.krecall.GlobalRecalling
import io.github.octestx.krecall.plugins.PluginManager
import io.github.octestx.krecall.repository.DataDB
import io.klogging.noCoLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import models.sqld.DataItem
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import ui.core.AbsUIPage

class TimestampViewPage(private val model: TimestampViewPageModel): AbsUIPage<Any?, TimestampViewPage.TimestampViewPageState, TimestampViewPage.TimestampViewPageAction>(model) {
    private val ologger = noCoLogger<TimestampViewPage>()
    @OptIn(ExperimentalResourceApi::class, ExperimentalMaterial3Api::class)
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
            var painter: Painter? by remember { mutableStateOf(null) }
            val loadingFailPainter = rememberVectorPainter(Icons.Default.Close)
            painter?.let {
                Image(
                    painter = it,
                    contentDescription = "Screen"
                )
            }
            LaunchedEffect(state.dataItem.timestamp) {
                painter = withContext(Dispatchers.IO) {
                    PluginManager.getStoragePlugin().getOrNull()?.getScreenData(state.dataItem.timestamp)?.let {
                        it.getOrNull()?.decodeToImageBitmap()?.let { img ->
                            BitmapPainter(img)
                        }
                    }?: loadingFailPainter
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
        val action: (TimestampViewPageAction) -> Unit
    ): AbsUIState<TimestampViewPageAction>()

    class TimestampViewPageModel(private val dataItem: DataItem, val highlightSearchStr: String? = null, private val goBack: () -> Unit): AbsUIModel<Any?, TimestampViewPageState, TimestampViewPageAction>() {
        val ologger = noCoLogger<TimestampViewPageModel>()

        @Composable
        override fun CreateState(params: Any?): TimestampViewPageState {
            return TimestampViewPageState(dataItem) {
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
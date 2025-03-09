package io.github.octestx.krecall.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.octestx.krecall.plugins.PluginManager
import io.github.octestx.krecall.repository.DataDB
import io.klogging.noCoLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import models.sqld.DataItem
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.InternalResourceApi
import ui.core.AbsUIPage

class SearchPage(model: SearchPageModel): AbsUIPage<Any?, SearchPage.SearchPageState, SearchPage.SearchPageAction>(model) {
    private val ologger = noCoLogger<SearchPage>()
    @OptIn(ExperimentalResourceApi::class, InternalResourceApi::class)
    @Composable
    override fun UI(state: SearchPageState) {
        Column {
            Row {
                TextField(state.searchText, { state.action(SearchPageAction.ChangeSearchText(it)) })
            }
            Box {

                LazyVerticalGrid(GridCells.FixedSize(100.dp), state = state.lazyGridState) {
                    items(state.searchResult, key = { it.timestamp }) { item ->
                        Card(Modifier.padding(6.dp).clickable {
                            state.action(SearchPageAction.JumpView(item))
                        }) {
                            val timestamp = item.timestamp
                            var imgBytes: ByteArray? by rememberSaveable { mutableStateOf(null) }
//                            painter?.let {
//                                Image(
//                                    painter = it,
//                                    contentDescription = "Screen"
//                                )
//                            }
                            imgBytes?.let {
                                AsyncImage(it, null)
                            }
                            LaunchedEffect(timestamp) {
                                imgBytes = withContext(Dispatchers.IO) {
                                    state.action(SearchPageAction.CheckImageCache)
                                    state.imageCache.getOrPut(timestamp) {
                                        PluginManager.getStoragePlugin().getOrNull()?.getScreenData(timestamp)?.let {
                                            it.getOrNull()
                                        }
                                    }
                                }
                            }
                            Text(text = item.data_?:"NULL", maxLines = 3)
                        }
                    }
                }
                VerticalScrollbar(
                    modifier = Modifier
                        .align(Alignment.CenterEnd) // 右侧对齐
                        .fillMaxHeight()
                        .background(Color.LightGray.copy(alpha = 0.5f)), // 半透明背景
                    adapter = rememberScrollbarAdapter(
                        scrollState = state.lazyGridState,
//                        itemCount = state.searchResult.size,
//                        averageItemSize = 100.dp // 根据实际项目高度调整
                    )
                )
            }
        }
    }
    sealed class SearchPageAction : AbsUIAction() {
        data class ChangeSearchText(val newText: String): SearchPageAction()
        data class JumpView(val dataItem: DataItem): SearchPageAction()
        data object CheckImageCache: SearchPageAction()
    }
    data class SearchPageState(
        val searchText: String,
        val searchResult: List<DataItem>,
        val lazyGridState: LazyGridState,
        val imageCache: MutableMap<Long, ByteArray?>,
        val action: (SearchPageAction) -> Unit
    ): AbsUIState<SearchPageAction>()

    class SearchPageModel(private val jumpView: (data: DataItem, search: String) -> Unit): AbsUIModel<Any?, SearchPageState, SearchPageAction>() {
        val ologger = noCoLogger<SearchPageModel>()
        private var _searchText by mutableStateOf("")
        private val _searchResultList = mutableStateListOf<DataItem>()
        private val _lazyGridState = LazyGridState()
        private val _imageCache = mutableMapOf<Long, ByteArray?>()

        @Composable
        override fun CreateState(params: Any?): SearchPageState {
            return SearchPageState(_searchText, _searchResultList, _lazyGridState, _imageCache) {
                actionExecute(params, it)
            }
        }
        override fun actionExecute(params: Any?, action: SearchPageAction) {
            when(action) {
                is SearchPageAction.ChangeSearchText -> {
                    _searchText = action.newText
                    search(action.newText)
                }
                is SearchPageAction.JumpView -> jumpView(action.dataItem, _searchText)
                SearchPageAction.CheckImageCache -> {
                    while (true) {
                        try {
                            if (_imageCache.size > 100) {
                                _imageCache.remove(_imageCache.keys.random())
                            } else break
                        } catch (e: Throwable) {
                            //Ignore
                        }
                    }
                }
            }
        }
        private fun search(text: String) {
            ologger.info { "Searching" }
            _searchResultList.clear()
            _searchResultList.addAll(DataDB.searchDataInAll(text))
            ologger.info { "Searched: ${_searchResultList.size}" }
        }
    }
}
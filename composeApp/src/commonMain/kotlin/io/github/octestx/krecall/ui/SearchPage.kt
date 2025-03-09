package io.github.octestx.krecall.ui

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.octestx.krecall.GlobalRecalling
import io.github.octestx.krecall.model.ImageState
import io.github.octestx.krecall.plugins.PluginManager
import io.github.octestx.krecall.repository.DataDB
import io.klogging.noCoLogger
import kotlinx.coroutines.withContext
import models.sqld.DataItem
import ui.core.AbsUIPage

class SearchPage(model: SearchPageModel): AbsUIPage<Any?, SearchPage.SearchPageState, SearchPage.SearchPageAction>(model) {
    private val ologger = noCoLogger<SearchPage>()
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
                            var imgState: ImageState by rememberSaveable { mutableStateOf(ImageState.Loading) }
                            when (imgState) {
                                ImageState.Error -> {
                                    Text("ERROR!")
                                }
                                ImageState.Loading -> {
                                    CircularProgressIndicator()
                                }
                                is ImageState.Success -> {
                                    AsyncImage((imgState as ImageState.Success).bytes, null, contentScale = ContentScale.Crop)
                                }
                            }
                            // ✅ 优化后的加载逻辑
                            LaunchedEffect(timestamp) {
                                if (GlobalRecalling.imageCache.containsKey(timestamp)) {
                                    imgState = ImageState.Success(GlobalRecalling.imageCache[timestamp]!!)
                                    return@LaunchedEffect
                                }

                                imgState = try {
                                    withContext(GlobalRecalling.imageLoadingDispatcher) {
                                        val bytes = GlobalRecalling.imageCache.getOrPut(timestamp) {
                                            PluginManager.getStoragePlugin().getOrNull()
                                                ?.getScreenData(timestamp)
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
                    )
                )
            }
        }
    }
    sealed class SearchPageAction : AbsUIAction() {
        data class ChangeSearchText(val newText: String): SearchPageAction()
        data class JumpView(val dataItem: DataItem): SearchPageAction()
    }
    data class SearchPageState(
        val searchText: String,
        val searchResult: List<DataItem>,
        val lazyGridState: LazyGridState,
        val action: (SearchPageAction) -> Unit
    ): AbsUIState<SearchPageAction>()

    class SearchPageModel(private val jumpView: (data: DataItem, search: String) -> Unit): AbsUIModel<Any?, SearchPageState, SearchPageAction>() {
        val ologger = noCoLogger<SearchPageModel>()
        private var _searchText by mutableStateOf("")
        private val _searchResultList = mutableStateListOf<DataItem>()
        private val _lazyGridState = LazyGridState()

        @Composable
        override fun CreateState(params: Any?): SearchPageState {
            return SearchPageState(_searchText, _searchResultList, _lazyGridState) {
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
package io.github.octestx.krecall.ui.tour

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import io.github.octestx.krecall.plugins.PluginManager
import io.github.octestx.krecall.plugins.basic.*
import io.klogging.noCoLogger
import ui.core.AbsUIPage

class LoadingPage(model: LoadingPageModel): AbsUIPage<Any?, LoadingPage.PluginConfigState, LoadingPage.LoadingPageAction>(model) {
    private val ologger = noCoLogger<LoadingPage>()
    @Composable
    override fun UI(state: PluginConfigState) {
        Column {
            CircularProgressIndicator()
        }
    }

    sealed class LoadingPageAction : AbsUIAction() {
    }
    data class PluginConfigState(
        val action: (LoadingPageAction) -> Unit,
    ): AbsUIState<LoadingPageAction>()

    class LoadingPageModel(): AbsUIModel<Any?, PluginConfigState, LoadingPageAction>() {
        val ologger = noCoLogger<LoadingPageModel>()
        @Composable
        override fun CreateState(params: Any?): PluginConfigState {
            return PluginConfigState() {
                actionExecute(params, it)
            }
        }
        override fun actionExecute(params: Any?, action: LoadingPageAction) {
        }
    }
}
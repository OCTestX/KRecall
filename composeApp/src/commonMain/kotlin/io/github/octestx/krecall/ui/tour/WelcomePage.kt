package io.github.octestx.krecall.ui.tour

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.kotlin.fibonacci.ui.toast
import io.github.octestx.krecall.plugins.PluginManager
import io.github.octestx.krecall.plugins.basic.*
import io.klogging.noCoLogger
import ui.core.AbsUIPage

class WelcomePage(model: WelcomePageModel): AbsUIPage<Any?, WelcomePage.PluginConfigState, WelcomePage.WelcomePageAction>(model) {
    private val ologger = noCoLogger<WelcomePage>()
    @Composable
    override fun UI(state: PluginConfigState) {
        Column {
            Text("Welcome!")
            Text("This is KRecall.")
            SelectionContainer {
                Text("Please visit website: TODO")
            }
            Button(onClick = {
                state.action(WelcomePageAction.Next)
            }) {
                Text("Next")
            }
        }
    }

    sealed class WelcomePageAction : AbsUIAction() {
        //只有当插件全部初始化完毕后用户才能主动调用这个事件
        data object Next: WelcomePageAction()
    }
    data class PluginConfigState(
        val action: (WelcomePageAction) -> Unit,
    ): AbsUIState<WelcomePageAction>()

    class WelcomePageModel(private val next: () -> Unit): AbsUIModel<Any?, PluginConfigState, WelcomePageAction>() {
        val ologger = noCoLogger<WelcomePageModel>()
        @Composable
        override fun CreateState(params: Any?): PluginConfigState {
            return PluginConfigState() {
                actionExecute(params, it)
            }
        }
        override fun actionExecute(params: Any?, action: WelcomePageAction) {
            when(action) {
                WelcomePageAction.Next -> {
                    next()
                }
            }
        }
    }
}
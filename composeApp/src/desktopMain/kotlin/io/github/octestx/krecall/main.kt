package io.github.octestx.krecall

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.application
import io.github.octestx.krecall.composeapp.generated.resources.Res
import io.github.octestx.krecall.composeapp.generated.resources.icon
import moe.tlaster.precompose.PreComposeWindow
import org.jetbrains.compose.resources.painterResource

fun main() = application {
    Core.init()
    val appMainPageModel = remember { AppMainPage.AppMainPageModel() }
    val appMainPage = remember { AppMainPage(appMainPageModel) }
    var windowVisible by remember { mutableStateOf(true) }
    PreComposeWindow(
        visible = windowVisible,
        onCloseRequest = {
            windowVisible = false
        },
        title = "KRecall",
    ) {
        appMainPage.Main(Unit)
    }
    // 创建系统托盘
    Tray(
        icon = painterResource(Res.drawable.icon),
        menu = {
            Item("恢复窗口", onClick = { windowVisible = true })
            Separator()
            Item("退出", onClick = ::exitApplication)
        }
    )
}
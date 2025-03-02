package io.github.octestx.krecall

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import moe.tlaster.precompose.PreComposeWindow

fun main() = application {
    Core.init()
    PreComposeWindow(
        onCloseRequest = ::exitApplication,
        title = "KRecall",
    ) {
        App()
    }
}
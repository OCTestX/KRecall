package io.github.octestx.krecall

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "KRecall",
    ) {
        App()
    }
}
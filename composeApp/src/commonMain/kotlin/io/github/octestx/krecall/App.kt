package io.github.octestx.krecall

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.kotlin.fibonacci.ui.BasicMUIWrapper
import io.github.octestx.krecall.ui.PluginConfigPage
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    BasicMUIWrapper {
        PluginConfigPage.Main(Unit)
    }
}
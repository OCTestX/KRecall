package io.github.octestx.krecall

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.kotlin.fibonacci.ui.BasicMUIWrapper
import io.github.octestx.krecall.ui.HomePage
import io.github.octestx.krecall.ui.PluginConfigPage
import moe.tlaster.precompose.PreComposeApp
import moe.tlaster.precompose.navigation.NavHost
import moe.tlaster.precompose.navigation.rememberNavigator
import moe.tlaster.precompose.navigation.transition.NavTransition
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    BasicMUIWrapper {
        PreComposeApp {
            val navigator = rememberNavigator()
            NavHost(
                // 将 Navigator 给到 NavHost
                navigator = navigator,
                // 定义初始导航路径
                initialRoute = "/pluginConfigPage",
                // 自定义页面导航动画，这个是个可选项
                navTransition = NavTransition(),
            ) {
                scene(
                    route = "/pluginConfigPage",
                    navTransition = NavTransition(),
                ) {
                val model = remember { PluginConfigPage.PluginConfigModel() }
                val page = remember {
                    PluginConfigPage(model) {
                        navigator.navigate("/home")
                    }
                }
                page.Main(Unit)
                }
                scene(
                    route = "/home",
                    navTransition = NavTransition(),
                ) {
                val model = remember { HomePage.PluginConfigModel() }
                val page = remember {
                    HomePage(model)
                }
                page.Main(Unit)
                }
            }
        }
    }
}
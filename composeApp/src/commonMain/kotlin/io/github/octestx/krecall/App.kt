package io.github.octestx.krecall

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.memory.MemoryCache
import coil3.request.crossfade
import io.github.kotlin.fibonacci.ui.BasicMUIWrapper
import io.github.octestx.krecall.repository.ConfigManager
import io.github.octestx.krecall.ui.*
import io.github.octestx.krecall.ui.tour.LoadingPage
import io.github.octestx.krecall.ui.tour.PluginConfigPage
import io.github.octestx.krecall.ui.tour.RecallSettingPage
import io.github.octestx.krecall.ui.tour.WelcomePage
import io.klogging.noCoLogger
import moe.tlaster.precompose.PreComposeApp
import moe.tlaster.precompose.navigation.*
import moe.tlaster.precompose.navigation.transition.NavTransition
import org.jetbrains.compose.ui.tooling.preview.Preview
import java.util.UUID

@Composable
@Preview
fun App() {
    val ologger = noCoLogger("@Composable_App")
    BasicMUIWrapper {
        PreComposeApp {
            // An alias of SingletonImageLoader.setSafe that's useful for
            // Compose Multiplatform apps.
            setSingletonImageLoaderFactory { context ->
                ImageLoader.Builder(context)
                    .crossfade(true)
                    .memoryCache {
                        MemoryCache.Builder()
                            // 设置缓存大小为 100MB
                            .maxSizeBytes(100L * 1024 * 1024)
                            .build()
                    }
                    .build()
            }

            val navigator = rememberNavigator()
            val navDataExchangeCache = mutableMapOf<String, Any?>()
            NavHost(
                // 将 Navigator 给到 NavHost
                navigator = navigator,
                // 定义初始导航路径
                initialRoute = "/loading",
                // 自定义页面导航动画，这个是个可选项
                navTransition = NavTransition(),
                modifier = Modifier
                    .fillMaxSize()
//                    .onKeyEvent { event ->
//                        when (event.key) {
//                            Key.Escape -> {
//                                if (true) {
//                                    navigator.popBackStack()
//                                    true  // 事件已处理
//                                } else {
//                                    false
//                                }
//                            }
//                            else -> false
//                        }
//                    },
            ) {
                scene(
                    route = "/loading",
                    navTransition = NavTransition(),
                ) {
                    val model = remember { LoadingPage.LoadingPageModel() }
                    val page = remember {
                        LoadingPage(model)
                    }
                    page.Main(Unit)
                }
                scene(
                    route = "/tour/welcome",
                    navTransition = NavTransition(),
                ) {
                    val model = remember { WelcomePage.WelcomePageModel(next = {
                        navigator.navigate("/tour/recallSetting")
                    }) }
                    val page = remember {
                        WelcomePage(model)
                    }
                    page.Main(Unit)
                }
                scene(
                    route = "/tour/recallSetting",
                    navTransition = NavTransition()
                ) {
                    val model = remember { RecallSettingPage.RecallSettingPageModel(next = {
                        navigator.navigate("/pluginConfigPage")
                    }) }
                    val page = remember {
                        RecallSettingPage(model)
                    }
                    page.Main(Unit)
                }
                scene(
                    route = "/pluginConfigPage",
                    navTransition = NavTransition(),
                ) {
                    val model = remember {
                        PluginConfigPage.PluginConfigModel() {
                            navigator.navigate("/home", options = NavOptions(popUpTo = PopUpTo.Prev))
                            ConfigManager.save(ConfigManager.config.copy(
                                initialized = true
                            ))
                        }
                    }
                    val page = remember {
                        PluginConfigPage(model)
                    }
                    page.Main(Unit)
                }
                scene(
                    route = "/home",
                    navTransition = NavTransition(),
                ) {
                    Column {
                        // Tab导航栏
                        var currentTabIndex by rememberSaveable { mutableStateOf(0) } // 当前选中Tab索引
                        TabRow(selectedTabIndex = currentTabIndex) {
                            Tab(
                                selected = currentTabIndex == 0,
                                onClick = { currentTabIndex = 0 }
                            ) {
                                Text("Home")
                            }
                            Tab(
                                selected = currentTabIndex == 1,
                                onClick = { currentTabIndex = 1 }
                            ) {
                                Text("Search")
                            }
                            val count = GlobalRecalling.errorTimestampCount.collectAsState().value
                            Tab(
                                selected = currentTabIndex == 2,
                                onClick = { currentTabIndex = 2 },
                                enabled = count > 0
                            ) {
                                AnimatedContent(count) {
                                    if (count > 0) {
                                        Text("ViewProcessFails: $it")
                                    } else {
                                        Text("No ViewProcessFails")
                                    }
                                }
                            }
                        }
                        val homeModel = rememberSaveable() { HomePage.HomePageModel() }
                        val homePage = rememberSaveable() { HomePage(homeModel) }

                        val searchModel = rememberSaveable() { SearchPage.SearchPageModel(jumpView = { data, search ->
                            val modelData = TimestampViewPage.TimestampViewPageModelData(data, search)
                            val modelDataId = UUID.randomUUID().toString()
                            navDataExchangeCache[modelDataId] = modelData
                            ologger.info { "SendModelDataId: $modelDataId" }
                            navigator.navigate("/timestampViewPage?modelDataId=$modelDataId")
                        }) }
                        val searchPage = rememberSaveable() { SearchPage(searchModel) }

                        val viewProcessFailsModel = rememberSaveable() { ViewProcessFailsPage.ViewProcessFailsPageModel(jumpView = { data, search ->
                            val modelData = TimestampViewPage.TimestampViewPageModelData(data, search)
                            val modelDataId = UUID.randomUUID().toString()
                            navDataExchangeCache[modelDataId] = modelData
                            ologger.info { "SendModelDataId: $modelDataId" }
                            navigator.navigate("/timestampViewPage?modelDataId=$modelDataId")
                        }) }
                        val viewProcessFailsPage = rememberSaveable() { ViewProcessFailsPage(viewProcessFailsModel) }
                        // 内容区域
                        when (currentTabIndex) {
                            0 -> {
                                homePage.Main(Unit)
                            }
                            1 -> {
                                searchPage.Main(Unit)
                            }
                            2 -> {
                                viewProcessFailsPage.Main(Unit)
                            }
                        }
                    }
                }
                scene(
                    route = "/timestampViewPage",
                    navTransition = NavTransition(),
                ) {
                    val modelDataId = it.query<String>("modelDataId")
                    LaunchedEffect(Unit) {
                        ologger.info { "ReceiveModelDataId: $modelDataId" }
                    }
                    val modelData: TimestampViewPage.TimestampViewPageModelData = navDataExchangeCache[modelDataId] as TimestampViewPage.TimestampViewPageModelData
                    val model = remember { TimestampViewPage.TimestampViewPageModel(modelData) {
                        navigator.goBack()
                    } }
                    val page = remember {
                        TimestampViewPage(model)
                    }
                    page.Main(Unit)
                }
            }




            LaunchedEffect(Unit) {
                if (ConfigManager.config.initialized) {
                    navigator.navigate("/home")
                } else {
                    navigator.navigate("/tour/welcome")
                }
            }
        }
    }
}
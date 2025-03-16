package io.github.octestx.krecall.plugins.impl.getscreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import io.github.octestx.krecall.plugins.basic.AbsGetScreenPlugin
import io.klogging.noCoLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import java.io.File
import java.io.FileNotFoundException
import java.io.OutputStream

class GetScreenByWinPowerShellPlugin: AbsGetScreenPlugin(pluginId = "GetScreenByWinPowerShellPlugin") {
    private val ologger = noCoLogger<GetScreenByWinPowerShellPlugin>()
    override suspend fun supportOutputToStream(): Boolean = false

    override suspend fun getScreen(outputStream: OutputStream) {
        throw UnsupportedOperationException()
    }

    override suspend fun getScreen(outputFileBitItNotExits: File) {
        withContext(Dispatchers.IO) {
            ologger.info { "getScreen: $outputFileBitItNotExits" }


            // 更安全的路径处理方案
            val safePath = outputFileBitItNotExits.absolutePath
                .replace("'", "''")  // 处理单引号
                .replace(" ", "` ")  // 处理空格

            val screenSTR = "\$" + "screen"
            val bitmapSTR = "\$" + "bitmap"
            val graphicsSTR = "\$" + "graphics"
            val psCommand = """
                Add-Type -AssemblyName System.Windows.Forms
                Add-Type -AssemblyName System.Drawing
                $screenSTR = [Windows.Forms.Screen]::PrimaryScreen.Bounds
                $bitmapSTR = New-Object Drawing.Bitmap $screenSTR.width, $screenSTR.height
                $graphicsSTR = [Drawing.Graphics]::FromImage($bitmapSTR)
                $graphicsSTR.CopyFromScreen([Drawing.Point]::Empty, [Drawing.Point]::Empty, $screenSTR.size)
                $bitmapSTR.Save('$safePath')  # 使用单引号包裹路径
            """.trimIndent()

            val processBuilder = ProcessBuilder(
                "powershell.exe",
                "-Command",
                psCommand
            )

            processBuilder.redirectErrorStream(true)
            val process = processBuilder.start()

            // 处理执行结果
            val exitCode = process.waitFor()
            if (exitCode != 0 || !outputFileBitItNotExits.exists()) {
                val errorOutput = process.errorStream.bufferedReader().readText()
                val err = RuntimeException("""
                    Screenshot failed! 
                    Exit code: $exitCode
                    Error: $errorOutput
                """.trimIndent())
                ologger.error(err) { "Screenshot failed" }
                throw err
            }
        }
    }

    override fun load() {
        ologger.info { "Loaded" }
    }

    override fun unload() {}

    @OptIn(ExperimentalResourceApi::class)
    @Composable
    override fun UI() {
        val scope = rememberCoroutineScope()
        var painter: Painter? by remember { mutableStateOf(null) }
        Column {
            Button(onClick = {
                scope.launch {
                    val f = test()
                    val img = f.inputStream().readAllBytes().decodeToImageBitmap()
                    painter = BitmapPainter(img)
                    ologger.info("Test: ${f.absolutePath}")
                }
            }) {
                Text("Test")
            }
            painter?.let { Image(it, contentDescription = null) }
        }
    }

    private suspend fun test(): File {
        val f = File(pluginDir, "test.png")
        getScreen(f)
        if (!f.exists()) {
            throw FileNotFoundException("testFile not found")
        }
        ologger.info("Test: ${f.absolutePath}")
        return f
    }


    override fun tryInitInner(): InitResult {
        ologger.info { "TryInit" }
        val e = runBlocking {
            try {
                test()
                null
            } catch (e: Exception) {
                e
            }
        }
        if (e == null) {
            _initialized.value = true
            return InitResult.Success
        } else {
            return InitResult.Failed(e)
        }
    }

    private val _initialized = MutableStateFlow(false)
    override val initialized: StateFlow<Boolean> = _initialized
}
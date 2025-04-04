package io.github.octestx.krecall.ui.utils

import io.github.kotlin.fibonacci.utils.OS
import java.awt.*
import java.awt.image.BufferedImage
import java.io.File
import java.io.InputStream
import javax.swing.SwingUtilities

object SystemMessager {
    /**
     * 显示系统通知（优先使用原生通知机制）
     * @param title 通知标题
     * @param message 通知内容
     * @param iconPath 图标资源路径（例如 "/icon.png"）
     * @param onClick 点击通知的回调（可选）
     * @return Boolean 是否成功显示通知
     */
    fun showSystemNotification(
        title: String,
        message: String,
        iconPath: String? = null,
        onClick: (() -> Unit)? = null
    ): Boolean {
        return when {
            isLinux() -> tryLinuxNativeNotification(title, message, iconPath)
                    || fallbackToAwtNotification(title, message, iconPath, onClick)
            isWindows() -> tryWindowsNativeNotification(title, message)
                    || fallbackToAwtNotification(title, message, iconPath, onClick)
            else -> fallbackToAwtNotification(title, message, iconPath, onClick)
        }
    }

    // region 原生通知实现
    private fun tryLinuxNativeNotification(
        title: String,
        message: String,
        iconPath: String?
    ): Boolean {
        return try {
            val cmd = mutableListOf("notify-send", "\""+title+"\"", "\""+message+"\"")

            // 处理图标资源（转换为临时文件）
            iconPath?.let { path ->
                getResourceAsFile(path)?.absolutePath?.let {
                    cmd += "-i"
                    cmd += it
                }
            }

            Runtime.getRuntime()
                .exec(cmd.toTypedArray())
                .waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    private const val FAC = "\$"

    private fun tryWindowsNativeNotification(
        title: String,
        message: String
    ): Boolean {
        return try {
            val psScript = """
                Add-Type -AssemblyName System.Windows.Forms
                \${FAC}notify = New-Object System.Windows.Forms.NotifyIcon
                \${FAC}notify.Icon = [System.Drawing.SystemIcons]::Information
                \${FAC}notify.BalloonTipTitle = "$title"
                \${FAC}notify.BalloonTipText = "$message"
                \${FAC}notify.Visible = \${FAC}true
                \${FAC}notify.ShowBalloonTip(5000)
                Start-Sleep -Seconds 5
                \${FAC}notify.Dispose()
            """.trimIndent()

            Runtime.getRuntime().exec(arrayOf(
                "powershell",
                "-Command",
                psScript
            )).waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }
    // endregion

    // region AWT 备用通知
    private fun fallbackToAwtNotification(
        title: String,
        message: String,
        iconPath: String?,
        onClick: (() -> Unit)?
    ): Boolean {
        if (!SystemTray.isSupported()) return false

        var success = false

        SwingUtilities.invokeLater {
            try {
                val tray = SystemTray.getSystemTray()
                val trayIcon = createTrayIcon(iconPath, onClick).apply {
                    tray.add(this)
                    displayMessage(title, message, TrayIcon.MessageType.INFO)
                }
                success = true
            } catch (e: Exception) {
                handleNotificationError(e)
            }
        }

        return success
    }

    private fun createTrayIcon(
        iconPath: String?,
        onClick: (() -> Unit)?
    ): TrayIcon {
        val image = iconPath?.let { loadImageResource(it) } ?: createDefaultImage()

        return TrayIcon(image, "App Notification").apply {
            isImageAutoSize = true
            onClick?.let { callback ->
                addActionListener { callback() }
            }
        }
    }
    // endregion

    // region 工具方法
    private fun isLinux(): Boolean {
        return OS.getCurrentOS() == OS.OperatingSystem.LINUX
    }

    private fun isWindows(): Boolean {
        return OS.getCurrentOS() == OS.OperatingSystem.WIN
    }

    private fun getResourceAsFile(path: String): File? {
        return try {
            val inputStream: InputStream = javaClass.getResourceAsStream(path)
                ?: return null
            val tempFile = File.createTempFile("temp_icon", ".png")
            tempFile.deleteOnExit()
            inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            null
        }
    }

    private fun loadImageResource(path: String): Image {
        val url = javaClass.getResource(path)
            ?: throw IllegalArgumentException("Icon resource not found: $path")
        return Toolkit.getDefaultToolkit().createImage(url)
    }

    private fun createDefaultImage(): Image {
        val image = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
        image.graphics.apply {
            color = Color.WHITE
            fillOval(0, 0, 16, 16)
        }
        return image
    }

    private fun handleNotificationError(e: Exception) {
        when (e) {
            is IllegalArgumentException -> System.err.println("Icon error: ${e.message}")
            is AWTException -> System.err.println("Tray error: ${e.message}")
            else -> System.err.println("Unexpected error: ${e.javaClass.simpleName}")
        }
    }
    // endregion
}
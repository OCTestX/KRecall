package io.github.octestx.krecall.plugins

import io.github.octestx.krecall.plugins.basic.PluginBasic
import io.github.octestx.krecall.plugins.capturescreen.CaptureScreenByAwtRobotPlugin
import io.github.octestx.krecall.plugins.capturescreen.CaptureScreenByKDESpectaclePlugin
import io.github.octestx.krecall.plugins.capturescreen.CaptureScreenByWinPowerShellPlugin
import io.github.octestx.krecall.plugins.impl.ocr.OCRByZhiPuPlugin
import io.github.octestx.krecall.plugins.impl.storage.OTStoragePlugin

actual fun getPlatformExtPlugins(): Set<PluginBasic> {
    //TODO
    return setOf()
}

actual fun getPlatformInnerPlugins(): Set<PluginBasic> {
    //TODO
    return setOf(
        CaptureScreenByAwtRobotPlugin(),
        CaptureScreenByKDESpectaclePlugin(),
        CaptureScreenByWinPowerShellPlugin(),

        OCRByZhiPuPlugin(),

        OTStoragePlugin(),
    )
}
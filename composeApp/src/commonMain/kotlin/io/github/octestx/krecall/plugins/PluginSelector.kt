package io.github.octestx.krecall.plugins

import io.github.kotlin.fibonacci.utils.OS
import io.github.octestx.krecall.plugins.basic.AbsGetScreenPlugin
import io.github.octestx.krecall.plugins.basic.AbsNaturalLanguageConverterPlugin
import io.github.octestx.krecall.plugins.basic.AbsScreenLanguageConverterPlugin
import io.github.octestx.krecall.plugins.basic.AbsStoragePlugin
import io.github.octestx.krecall.plugins.impl.getscreen.GetScreenByAwtRobotPlugin
import io.github.octestx.krecall.plugins.impl.getscreen.GetScreenByWinPowerShellPlugin
import io.github.octestx.krecall.plugins.impl.getscreen.GetScreenByKDESpectaclePlugin
import io.github.octestx.krecall.plugins.impl.naturallanguage.NaturalLanguageConverterByKimiPlugin
import io.github.octestx.krecall.plugins.impl.screenlanguage.ScreenLanguageConverterByZhiPuPlugin
import io.github.octestx.krecall.plugins.impl.storage.OTStoragePlugin

object PluginSelector {
    fun recommendGetScreenPlugin(): AbsGetScreenPlugin {
        val os = OS.getCurrentOS()
        return when (os) {
            OS.OperatingSystem.WIN -> TODO()
            OS.OperatingSystem.MACOS -> TODO()
            OS.OperatingSystem.LINUX -> getScreenPlugin("GetScreenByKDESpectaclePlugin").getOrThrow()
            OS.OperatingSystem.OTHER -> TODO()
        }
    }
    fun recommendStoragePlugin(): AbsStoragePlugin {
        return storagePlugin("OTStoragePlugin").getOrThrow()
    }
    fun recommendScreenLanguageConverterPlugin(): AbsScreenLanguageConverterPlugin {
        return screenLanguageConverterPlugin("ScreenLanguageConverterByKimiPlugin").getOrThrow()
    }
    fun recommendNaturalLanguageConverterPlugin(): AbsNaturalLanguageConverterPlugin {
        return naturalLanguageConverterPlugin("NaturalLanguageConverterByKimiPlugin").getOrThrow()
    }

    //TODO Support more plugin
    val plugins = mapOf(
        "GetScreenByKDESpectaclePlugin" to GetScreenByKDESpectaclePlugin(),
        "GetScreenByAwtRobotPlugin" to GetScreenByAwtRobotPlugin(),
        "GetScreenByWinPowerShellPlugin" to GetScreenByWinPowerShellPlugin(),
        "NaturalLanguageConverterByKimiPlugin" to NaturalLanguageConverterByKimiPlugin(),
        "ScreenLanguageConverterByKimiPlugin" to ScreenLanguageConverterByZhiPuPlugin(),
        "OTStoragePlugin" to OTStoragePlugin(),
    )
    fun getScreenPlugin(id: String): Result<AbsGetScreenPlugin> = kotlin.runCatching {
        plugins[id] as AbsGetScreenPlugin
    }
    fun storagePlugin(id: String): Result<AbsStoragePlugin> = kotlin.runCatching {
        plugins[id] as AbsStoragePlugin
    }
    fun screenLanguageConverterPlugin(id: String): Result<AbsScreenLanguageConverterPlugin> = kotlin.runCatching {
        plugins[id] as AbsScreenLanguageConverterPlugin
    }
    fun naturalLanguageConverterPlugin(id: String): Result<AbsNaturalLanguageConverterPlugin> = kotlin.runCatching {
        plugins[id] as AbsNaturalLanguageConverterPlugin
    }
}
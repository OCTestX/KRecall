package io.github.octestx.krecall.plugins.basic

import java.io.BufferedOutputStream

/**
 * A parameterless constructor is required when inheriting a plugin.
 */
abstract class AbsCaptureAudioPlugin(pluginId: String): PluginBasic(pluginId) {
    /**
     * 在init结束后会被立刻调用
     */
    abstract fun provideReceiver(receiver: AudioDataReceiver)
    abstract fun start(outputStream: BufferedOutputStream)
    abstract fun pause()
    abstract val isCapturing: Boolean
    interface AudioDataReceiver {
        fun receive(data: ByteArray)
    }
}
package io.github.octestx.krecall.plugins.impl.screenlanguage

import androidx.compose.runtime.Composable
import io.github.kotlin.fibonacci.utils.ojson
import io.github.octestx.krecall.plugins.basic.AbsScreenLanguageConverterPlugin
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*

class ScreenLanguageConverterByKimiPlugin: AbsScreenLanguageConverterPlugin("ScreenLanguageConverterByKimiPlugin") {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) { ojson }
    }
    override suspend fun convert(screen: ByteArray): String {
        TODO("Not yet implemented")
    }

    override fun loadInner() {
        TODO("Not yet implemented")
    }

    @Composable
    override fun UI() {
        TODO("Not yet implemented")
    }

    override fun tryInitInner(): Exception? {
        TODO()
        initialized = true
        return null
    }

    override var initialized: Boolean = false
}
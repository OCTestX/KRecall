package io.github.octestx.krecall.plugins.impl.naturallanguage

import androidx.compose.runtime.Composable
import io.github.octestx.krecall.plugins.basic.AbsNaturalLanguageConverterPlugin

class NaturalLanguageConverterByKimiPlugin: AbsNaturalLanguageConverterPlugin("NaturalLanguageConverterByKimiPlugin") {
    override suspend fun convert(natural: String): String {
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
package io.github.octestx.krecall

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
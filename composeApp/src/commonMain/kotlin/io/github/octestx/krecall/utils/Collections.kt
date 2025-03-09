package io.github.octestx.krecall.utils

import java.util.*
import kotlin.collections.LinkedHashMap

// 添加线程安全扩展（文件底部）：
fun <K, V> LinkedHashMap<K, V>.synchronized() = Collections.synchronizedMap(this)
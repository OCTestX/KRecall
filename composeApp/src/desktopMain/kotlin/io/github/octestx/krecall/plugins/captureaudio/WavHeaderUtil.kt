package io.github.octestx.krecall.plugins.captureaudio

import java.io.DataOutputStream
import java.io.File
import java.io.OutputStream
import java.io.RandomAccessFile
import javax.sound.sampled.AudioFormat

object WavHeaderUtil2 {
    fun writeHeader(outputStream: OutputStream, format: AudioFormat) {
        DataOutputStream(outputStream).let { dos ->
            // RIFF 头
            dos.writeBytes("RIFF")
            dos.writeIntLittleEndian(0) // RIFF 块长度占位（后续更新）

            dos.writeBytes("WAVE")

            // fmt 子块
            dos.writeBytes("fmt ")
            dos.writeIntLittleEndian(16) // fmt 块长度（固定 16 字节）

            // 编码格式（PCM = 1）
            dos.writeShortLittleEndian(1)

            // 声道数（需转换为无符号 short）
            dos.writeShortLittleEndian(format.channels.toInt())

            // 采样率（小端 32 位）
            dos.writeIntLittleEndian(format.sampleRate.toInt())

            // 字节率（每秒字节数 = 采样率 * 声道数 * (位深/8)）
            val byteRate = (format.sampleRate * format.channels * (format.sampleSizeInBits / 8)).toInt()
            dos.writeIntLittleEndian(byteRate)

            // 块对齐（每帧字节数 = 声道数 * (位深/8)）
            val blockAlign = (format.channels * (format.sampleSizeInBits / 8)).toInt()
            dos.writeShortLittleEndian(blockAlign)

            // 位深度（如 16-bit）
            dos.writeShortLittleEndian(format.sampleSizeInBits)

            // data 子块
            dos.writeBytes("data")
            dos.writeIntLittleEndian(0) // data 块长度占位（后续更新）
        }
    }

    // 辅助函数：以小端序写入 Int
    private fun DataOutputStream.writeIntLittleEndian(value: Int) {
        write(byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte()
        ))
    }

    // 辅助函数：以小端序写入 Short（通过 Int 传入）
    private fun DataOutputStream.writeShortLittleEndian(value: Int) {
        require(value in 0..65535) { "Value out of short range: $value" }
        write(byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte()
        ))
    }

    fun updateHeader(file: File) {
        RandomAccessFile(file, "rw").use { raf ->
            val fileSize = file.length()
            // RIFF 块长度 = 文件总长度 - 8
            raf.seek(4)
            raf.writeIntLittleEndian((fileSize - 8).toInt())
            // data 块长度 = 文件总长度 - 44
            raf.seek(40)
            raf.writeIntLittleEndian((fileSize - 44).toInt())
        }
    }

    // 辅助函数：以小端序写入 Int
    private fun RandomAccessFile.writeIntLittleEndian(value: Int) {
        writeByte((value and 0xFF).toInt())
        writeByte(((value shr 8) and 0xFF).toInt())
        writeByte(((value shr 16) and 0xFF).toInt())
        writeByte(((value shr 24) and 0xFF).toInt())
    }
}
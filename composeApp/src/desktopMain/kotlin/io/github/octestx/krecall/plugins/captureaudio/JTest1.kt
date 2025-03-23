package io.github.octestx.krecall.plugins.captureaudio

import java.io.*
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.TargetDataLine

object WavHeaderUtil {
    // 写入 WAV 文件头（占位）
    fun writeHeader(dos: DataOutputStream, format: AudioFormat) {
        // RIFF 头
        dos.writeBytes("RIFF")
        dos.writeIntLittleEndian(0) // 占位：总长度（后续更新）
        dos.writeBytes("WAVE")

        // fmt 子块
        dos.writeBytes("fmt ")
        dos.writeIntLittleEndian(16) // fmt 块长度（固定 16 字节）
        dos.writeShortLittleEndian(1) // PCM 格式
        dos.writeShortLittleEndian(format.channels.toInt()) // 声道数
        dos.writeIntLittleEndian(format.sampleRate.toInt()) // 采样率
        // 字节率 = 采样率 × 声道数 × (位深/8)
        val byteRate = (format.sampleRate * format.channels * (format.sampleSizeInBits / 8)).toInt()
        dos.writeIntLittleEndian(byteRate)
        // 块对齐 = 声道数 × (位深/8)
        val blockAlign = (format.channels * (format.sampleSizeInBits / 8)).toInt()
        dos.writeShortLittleEndian(blockAlign)
        dos.writeShortLittleEndian(format.sampleSizeInBits) // 位深

        // data 子块
        dos.writeBytes("data")
        dos.writeIntLittleEndian(0) // 占位：数据长度（后续更新）
    }

    // 更新文件头中的长度字段
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

    // 小端序写入 Int
    private fun DataOutputStream.writeIntLittleEndian(value: Int) {
        write(byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte()
        ))
    }

    // 小端序写入 Short（通过 Int 参数）
    private fun DataOutputStream.writeShortLittleEndian(value: Int) {
        require(value in 0..65535) { "Value out of range: $value" }
        write(byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte()
        ))
    }
}

fun recordToWav(filePath: String, durationSeconds: Int) {
    // 配置音频格式（关键参数必须与硬件兼容）
    val format = AudioFormat(
        AudioFormat.Encoding.PCM_SIGNED,
        44100f,     // 采样率
        16,         // 位深
        2,          // 声道数（立体声）
        (16 / 8) * 2, // 帧大小（字节）
        44100f,     // 帧率
        false       // 小端序
    )

    val line = AudioSystem.getTargetDataLine(format) as TargetDataLine
    line.open(format)

    val file = File(filePath)
    val outputStream = DataOutputStream(BufferedOutputStream(FileOutputStream(file)))

    // 写入初始文件头（占位）
    WavHeaderUtil.writeHeader(outputStream, format)

    line.start()
    val buffer = ByteArray(4096)
    var totalBytesRead = 0
    val startTime = System.currentTimeMillis()

    try {
        // 精确控制录制时长
        while (System.currentTimeMillis() - startTime < durationSeconds * 1000) {
            val bytesRead = line.read(buffer, 0, buffer.size)
            outputStream.write(buffer, 0, bytesRead)
            totalBytesRead += bytesRead
        }
    } finally {
        // 确保资源释放
        line.stop()
        line.close()
        outputStream.close()
        // 更新文件头中的长度字段
        WavHeaderUtil.updateHeader(file)
        println("录制完成，实际写入数据大小：${totalBytesRead} 字节")
    }
}

// 使用示例：录制 5 秒音频
fun main() {
    val mixers = AudioSystem.getMixerInfo()
    mixers.forEach { mixerInfo ->
        val mixer = AudioSystem.getMixer(mixerInfo)
        val lineInfos = mixer.targetLineInfo
        lineInfos.forEach { lineInfo ->
            if (lineInfo.lineClass == TargetDataLine::class.java) {
                println("找到麦克风设备: ${mixerInfo.name}[${mixerInfo.vendor}]")
            }
        }
    }
    recordToWav("/home/octest/Myself/Project/AllFarmwork/Python/CodeGen/KRecall/composeApp/KRecall/data/Plugins/data/CaptureMicByJavaSoundPlugin/t1.wav", 5)
}
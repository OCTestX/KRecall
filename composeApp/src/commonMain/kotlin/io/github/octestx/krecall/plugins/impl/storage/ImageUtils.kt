package io.github.octestx.krecall.plugins.impl.storage

import java.awt.Color
import java.awt.Image
import java.awt.image.BufferedImage
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import javax.imageio.ImageIO
import java.io.ByteArrayInputStream
import org.apache.commons.math3.transform.*
import kotlin.math.roundToInt

object ImageUtils {
    // ================== 公共函数 ==================
    fun calculateImageSimilarity(image1Bytes: ByteArray, image2Bytes: ByteArray): Double {
        val img1 = ImageIO.read(ByteArrayInputStream(image1Bytes)) ?: throw IllegalArgumentException("Invalid image1 bytes")
        val img2 = ImageIO.read(ByteArrayInputStream(image2Bytes)) ?: throw IllegalArgumentException("Invalid image2 bytes")

        val hash1 = generatePHash(img1)
        val hash2 = generatePHash(img2)

        return (1 - calculateHammingDistance(hash1, hash2) / 64.0) * 100
    }

    // ================== 核心算法实现 ==================
    private fun generatePHash(image: BufferedImage): String {
        // 1. 缩放至32x32并灰度化
        val grayMatrix = resizeAndGrayScale(image, 32, 32)

        // 2. DCT变换并取左上8x8
        val dctMatrix = calculate2DDCT(grayMatrix).let { matrix ->
            Array(8) { i -> DoubleArray(8) { j -> matrix[i][j] } }
        }

        // 3. 手动展平矩阵（修复点）
        val flattened = mutableListOf<Double>().apply {
            dctMatrix.forEach { row -> row.forEach { add(it) } }
        }

        // 4. 排除DC系数（第一个元素）
        val values = flattened.subList(1, 64)

        // 5. 生成二进制哈希
        val avg = values.average()
        return values.joinToString("") { if (it > avg) "1" else "0" }
    }


    // ================== 图像处理工具 ==================
    private fun resizeAndGrayScale(src: BufferedImage, width: Int, height: Int): Array<DoubleArray> {
        // 高质量缩放
        val scaled = BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY).apply {
            createGraphics().run {
                drawImage(src.getScaledInstance(width, height, Image.SCALE_SMOOTH), 0, 0, null)
                dispose()
            }
        }

        // 提取灰度矩阵（0.0-1.0范围）
        return Array(height) { y ->
            DoubleArray(width) { x ->
                Color(scaled.getRGB(x, y)).run {
                    (0.299 * red + 0.587 * green + 0.114 * blue) / 255.0
                }
            }
        }
    }

    // ================== DCT变换实现 ==================
    private fun calculate2DDCT(matrix: Array<DoubleArray>): Array<DoubleArray> {
        val transformer = FastCosineTransformer(DctNormalization.STANDARD_DCT_I)

        // 行变换
        val rowsDCT = matrix.map { row ->
            transformer.transform(row, TransformType.FORWARD)
        }.toTypedArray()

        // 列变换
        return Array(rowsDCT[0].size) { col ->
            DoubleArray(rowsDCT.size) { row -> rowsDCT[row][col] }
        }.map { col ->
            transformer.transform(col, TransformType.FORWARD)
        }.let { cols ->
            Array(cols[0].size) { row ->
                DoubleArray(cols.size) { col -> cols[col][row] }
            }
        }
    }

    // ================== 汉明距离计算 ==================
    private fun calculateHammingDistance(hash1: String, hash2: String): Int {
        require(hash1.length == hash2.length) { "Hashes must have same length" }
        return hash1.indices.count { hash1[it] != hash2[it] }
    }
}
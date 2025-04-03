package io.github.octestx.krecall.plugins.ocr.ocrlibrary

//class OCRComposeFront(private val port: Int) {
//    val client = HttpClient(CIO)
//    suspend fun detectFull(
//        image: File,
//        name: String = System.nanoTime().toString() + ".png",
//        maxSideLen: Int = OcrEngine.maxSideLen,
//        padding: Int = OcrEngine.padding,
//        boxScoreThresh: Float = OcrEngine.boxScoreThresh,
//        boxThresh: Float = OcrEngine.boxThresh,
//        unClipRatio: Float = OcrEngine.unClipRatio,
//        doAngle: Boolean = OcrEngine.doAngle,
//        mostAngle: Boolean = OcrEngine.mostAngle,
//    ): OcrResult {
//        WorkingDir.ocrDir.link(name).writeBytes(image.readBytes())
//        val response: HttpResponse = client.request("http://localhost:$port/ocrDir/$name")
//        return Json.decodeFromString(response.bodyAsText())
//    }
//
//    suspend fun detect(image: File, name: String = System.nanoTime().toString() + ".png"): String {
//        WorkingDir.ocrDir.link(name).writeBytes(image.readBytes())
//        val response: HttpResponse = client.request("http://localhost:$port/justText/ocrDir/$name")
//        return response.bodyAsText()
//    }
//}
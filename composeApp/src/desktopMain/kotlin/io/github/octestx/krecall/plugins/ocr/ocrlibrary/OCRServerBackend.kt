package io.github.octestx.krecall.plugins.ocr.ocrlibrary

//import data.WorkingDir
//import io.github.octestx.krecall.plugins.ocr.ocrlibrary.OCRCore
//import io.ktor.server.application.*
//import io.ktor.server.engine.*
//import io.ktor.server.netty.*
//import io.ktor.server.response.*
//import io.ktor.server.routing.*
//import kotlinx.serialization.json.Json
//import java.io.File
//
//object OCRServerBackend {
//    val ocrCore = OCRCore()
//    fun start(port: Int) {
//        embeddedServer(Netty, port = port) {
//            routing {
//                get("/ocrDir/{fileName}") {
//                    val fileName = call.parameters["fileName"]!!
//                    val result = ocrCore.detect(File(WorkingDir.ocrDir, fileName))
//                    println(result)
//                    call.respondText(Json.encodeToString(result))
//                }
//                get("justText/ocrDir/{fileName}") {
//                    val fileName = call.parameters["fileName"]!!
//                    val result = ocrCore.detect(File(WorkingDir.ocrDir, fileName))
//                    println(result.strRes)
//                    call.respondText(result.strRes)
//                }
//            }
//        }.start(wait = true)
//    }
//}
//const val ocrPort = 18856
//private fun main() {
//    OCRServerBackend.start(ocrPort)
//}
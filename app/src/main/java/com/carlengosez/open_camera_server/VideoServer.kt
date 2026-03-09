package com.carlengosez.open_camera_server

import android.os.Environment
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.serialization.gson.gson
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import java.io.File

class VideoServer {

    fun startServer() {
        Thread {
            try {
                embeddedServer(Netty, port = 8080) {

                    install(CORS) {
                        anyHost()
                    }
                    install(ContentNegotiation) {
                        gson()
                    }

                    routing {

                        get("/videos") {
                            val folder = File(
                                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                                "CameraServer-Videos"
                            )

                            if (!folder.exists()) folder.mkdirs()

                            // AQUÍ ESTÁ EL CAMBIO: Ignoramos los archivos que empiezan con un punto (.)
                            val videoFiles = folder.listFiles()
                                ?.filter { it.extension == "mp4" && !it.name.startsWith(".") }
                                ?.sortedByDescending { it.lastModified() }
                                ?.map { it.name } ?: emptyList()

                            call.respond(videoFiles)
                        }

                        get("/video/{fileName}") {
                            val fileName = call.parameters["fileName"]
                            val file = File(
                                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                                "CameraServer-Videos/$fileName"
                            )

                            if (file.exists()) {
                                call.respondFile(file)
                            } else {
                                call.respond(HttpStatusCode.NotFound, "El video no existe.")
                            }
                        }
                    }
                }.start(wait = true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}
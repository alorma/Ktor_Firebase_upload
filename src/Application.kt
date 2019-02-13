package com.example

import com.google.cloud.storage.BlobId
import com.google.firebase.cloud.StorageClient
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.authenticate
import io.ktor.auth.basic
import io.ktor.features.CallLogging
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.request.path
import io.ktor.request.receiveMultipart
import io.ktor.response.respond
import io.ktor.routing.post
import io.ktor.routing.routing
import org.slf4j.event.Level
import java.io.File

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }

    install(FirebaseConfig) {
        fileName = "resources/service_account.json"
    }

    install(Authentication) {
        basic("myBasicAuth") {
            realm = "Ktor Server"
            validate { UserIdPrincipal(it.name) }
        }
    }

    routing {
        authenticate("myBasicAuth") {
            post("/upload") {
                val multipart = call.receiveMultipart()

                multipart.forEachPart { part ->
                    when (part) {
                        is PartData.FileItem -> {
                            val id = uploadFile(part)
                            call.respond(HttpStatusCode.Created, "Image id: $id")
                        }
                        else -> {

                        }
                    }
                    part.dispose()
                }
            }
        }
    }
}

fun uploadFile(part: PartData.FileItem): BlobId {
    val file = File(part.originalFileName)
    val fileName = "${file.nameWithoutExtension}_${System.currentTimeMillis()}.${file.extension}"
    return part.streamProvider().use { input ->
        val bucket = StorageClient.getInstance().bucket()
        val blob = bucket.create(fileName, input)
        blob.blobId
    }
}

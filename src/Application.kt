package com.example

import com.fasterxml.jackson.databind.SerializationFeature
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.Storage
import com.google.firebase.cloud.StorageClient
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.*
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.jackson.jackson
import io.ktor.request.path
import io.ktor.request.receiveMultipart
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import org.slf4j.event.Level
import java.io.File
import java.net.URL
import java.util.concurrent.TimeUnit

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

    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }

    routing {
        authenticate("myBasicAuth") {
            get("/images") {
                val principal = call.principal<UserIdPrincipal>()!!
                val bucket = StorageClient.getInstance().bucket()

                val options = Storage.BlobListOption.prefix(principal.name)

                val items = bucket.list(options).values.map {
                    it.signUrl(10, TimeUnit.MINUTES)
                }
                call.respond(items)
            }
            post("/upload") {
                val principal = call.principal<UserIdPrincipal>()!!
                call.receiveMultipart().forEachPart { part ->
                    when (part) {
                        is PartData.FileItem -> {
                            val image = uploadFile(part, principal.name)
                            // TODO store image.id
                            call.respond(HttpStatusCode.Created, image.url)
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

fun uploadFile(part: PartData.FileItem, userName: String): Image {
    val file = File(part.originalFileName)
    val fileName = "$userName/${file.nameWithoutExtension}_${System.currentTimeMillis()}.${file.extension}"
    return part.streamProvider().use { input ->
        val bucket = StorageClient.getInstance().bucket()
        val blob = bucket.create(fileName, input)
        Image(blob.blobId, blob.signUrl(10, TimeUnit.MINUTES))
    }
}

data class Image(val id: BlobId, val url: URL)
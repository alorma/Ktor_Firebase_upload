package com.example

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.authenticate
import io.ktor.auth.basic
import io.ktor.features.CallLogging
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.request.path
import io.ktor.request.receiveMultipart
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
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

    install(Authentication) {
        basic("myBasicAuth") {
            realm = "Ktor Server"
            validate { UserIdPrincipal(it.name) }
        }
    }

    routing {
        get("/") {
            call.respondText("HELLO WORLD!", contentType = ContentType.Text.Plain)
        }

        authenticate("myBasicAuth") {
            post("/upload") {
                val multipart = call.receiveMultipart()

                multipart.forEachPart { part ->
                    when (part) {
                        is PartData.FileItem -> {
                            uploadFile(part)
                        }
                        else -> {

                        }
                    }
                    part.dispose()
                }
                call.respond(HttpStatusCode.Created)
            }
        }
    }
}

fun uploadFile(part: PartData.FileItem) {
    val name = File(part.originalFileName).nameWithoutExtension
    val ext = File(part.originalFileName).extension
    println("File name: $name.$ext")
}

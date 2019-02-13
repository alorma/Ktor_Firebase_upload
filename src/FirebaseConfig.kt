package com.example

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineContext
import java.io.File
import java.io.FileInputStream

/**
 * Adds firebase configuration.
 */
class FirebaseConfig(configuration: Configuration) {

    private val fileName = configuration.fileName

    class Configuration {
        lateinit var fileName: String
    }

    // Body of the feature
    private fun intercept(context: PipelineContext<Unit, ApplicationCall>) {
        try {
            FirebaseApp.getInstance()
        } catch (e: IllegalStateException) {
            initFirebase()
        }
    }

    private fun initFirebase() {
        FileInputStream(File(fileName))
            .let {
                GoogleCredentials.fromStream(it)
            }.let {
                FirebaseOptions.Builder()
                    .setCredentials(it)
                    .setStorageBucket("lostin-81e79.appspot.com")
                    .build()
            }.let {
                FirebaseApp.initializeApp(it)
            }
    }

    /**
     * Installable feature for [FirebaseConfig].
     */
    companion object Feature :
        ApplicationFeature<ApplicationCallPipeline, Configuration, FirebaseConfig> {
        override val key = AttributeKey<FirebaseConfig>("FirebaseConfig")
        override fun install(
            pipeline: ApplicationCallPipeline,
            configure: Configuration.() -> Unit
        ): FirebaseConfig {
            val configuration = Configuration().apply(configure)
            val feature = FirebaseConfig(configuration)
            pipeline.intercept(ApplicationCallPipeline.Call) {
                feature.intercept(this)
            }
            return feature
        }
    }
}
package manutenzioni.service

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.http.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import manutenzioni.app.strategy.HtmlToPdfStrategy
import manutenzioni.domain.model.*
import java.io.File
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

@Serializable
data class GeneratePdfRequest(
    val impianto: Impianto,
    val frequenza: Periodo,
    val clienteNome: String? = null
)

fun Application.module() {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            prettyPrint = true
            isLenient = true
            classDiscriminator = "_t"
            serializersModule = SerializersModule {
                polymorphic(Impianto::class) {
                    subclass(ImpiantoStandard::class)
                    subclass(QuadroBT::class)
                    subclass(ImpiantoEmergenza::class)
                    subclass(RilevazioneAntincendio::class)
                    subclass(RilevazioneGas::class)
                    subclass(QuadroMQT::class)
                }
            }
        })
    }

    val pdfStrategy = HtmlToPdfStrategy()

    routing {
        post("/api/generate-pdf") {
            try {
                val request = call.receive<GeneratePdfRequest>()

                // Create a temporary output file
                val tempDir = File.createTempFile("pdf-gen-dir", "")
                tempDir.delete()
                tempDir.mkdir()

                // Use the generateBatch since generate is protected
                val batchResult = pdfStrategy.generateBatch(
                    impianto = request.impianto,
                    frequenza = request.frequenza,
                    copies = 1,
                    outputDir = tempDir,
                    clienteNome = request.clienteNome,
                    contextImpianti = listOf(request.impianto)
                ) { _, _ ->
                    // do nothing
                }

                val successFiles = batchResult.generatedFiles
                if (successFiles.isNotEmpty()) {
                    val fileToRespond = successFiles.first()
                    // Serve the PDF
                    call.response.header(
                        HttpHeaders.ContentDisposition,
                        ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, "scheda.pdf").toString()
                    )
                    call.respondFile(fileToRespond)
                } else {
                    val errors = batchResult.errors
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to generate PDF: ${errors}"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Unknown error")))
            }
        }
    }
}

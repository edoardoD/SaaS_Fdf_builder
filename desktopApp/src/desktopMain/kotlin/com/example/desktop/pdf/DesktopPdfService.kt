package com.example.desktop.pdf

import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.example.domain.Cantiere
import java.io.File

class DesktopPdfService {
    fun sampleCreatePdf(path: String) {
        val file = File(path)
        file.parentFile?.mkdirs()
        
        val writer = PdfWriter(path)
        val pdf = PdfDocument(writer)
        val document = Document(pdf)
        document.add(Paragraph("Hello iText7 from Kotlin Multiplatform Desktop!"))
        document.add(Paragraph("Generated at: ${java.util.Date()}"))
        document.close()
    }

    fun generatePdfsForCantiere(cantiere: Cantiere, frequenza: String, basePath: String): List<String> {
        val generatedFiles = mutableListOf<String>()
        val baseDir = File(basePath)
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }

        // Filtra gli apparati per la frequenza richiesta
        val apparatiFiltrati = cantiere.apparati.filter { it.frequenza.equals(frequenza, ignoreCase = true) }

        for (apparato in apparatiFiltrati) {
            // Crea un file PDF separato per ogni apparato
            val safeApparatoNome = apparato.nome.replace(Regex("[^A-Za-z0-9 ]"), "").replace(" ", "_")
            val filePath = File(baseDir, "Scheda_${cantiere.nome}_${safeApparatoNome}_${apparato.id}.pdf").absolutePath

            sampleCreatePdf(filePath) // Use existing basic generation, in a real scenario you would fill AcroForm here
            generatedFiles.add(filePath)
        }

        return generatedFiles
    }
}

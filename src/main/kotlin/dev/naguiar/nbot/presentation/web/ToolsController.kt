package dev.naguiar.nbot.presentation.web

import com.prowidesoftware.swift.model.mx.MxCamt05300102
import dev.naguiar.nbot.budget.application.CamtMergerService
import dev.naguiar.nbot.budget.domain.CamtFilter
import jakarta.servlet.http.HttpSession
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile
import java.util.*

@RequestMapping("/dashboard/tools")
@Controller
class ToolsController(
    private val camtMergerService: CamtMergerService,
) {
    @GetMapping
    fun toolsFragment(): String = "fragments/tools :: tools"

    @PostMapping("/merge-preview")
    fun mergePreview(
        @RequestParam("file") file: MultipartFile,
        session: HttpSession,
        model: Model,
    ): String {
        if (!file.isEmpty) {
            val documents = camtMergerService.parseZipToDocuments(file.inputStream)
            session.setAttribute("mergePreviewDocs", documents)

            val previews = camtMergerService.getPreviewsFromDocuments(documents)
            model.addAttribute("previews", previews)
        }
        return "fragments/tools :: preview"
    }

    @PostMapping("/filters")
    fun saveFilter(
        @RequestParam(required = false) namePattern: String?,
        @RequestParam(required = false) ibanPattern: String?,
        @RequestParam(defaultValue = "false") isStrict: Boolean,
        session: HttpSession,
        model: Model,
    ): String {
        if (!namePattern.isNullOrBlank() || !ibanPattern.isNullOrBlank()) {
            camtMergerService.saveFilter(
                CamtFilter(
                    namePattern = namePattern?.takeIf { it.isNotBlank() },
                    ibanPattern = ibanPattern?.takeIf { it.isNotBlank() },
                    isStrict = isStrict,
                ),
            )
        }

        @Suppress("UNCHECKED_CAST")
        val documents = session.getAttribute("mergePreviewDocs") as? List<MxCamt05300102>
        if (documents != null) {
            val previews = camtMergerService.getPreviewsFromDocuments(documents)
            model.addAttribute("previews", previews)
            model.addAttribute("filterSuccess", "Filter added successfully!")
        }

        return "fragments/tools :: preview"
    }

    @PostMapping("/filters/delete/{id}")
    fun deleteFilter(
        @PathVariable id: UUID,
        session: HttpSession,
        model: Model,
    ): String {
        camtMergerService.deleteFilter(id)

        @Suppress("UNCHECKED_CAST")
        val documents = session.getAttribute("mergePreviewDocs") as? List<MxCamt05300102>
        if (documents != null) {
            val previews = camtMergerService.getPreviewsFromDocuments(documents)
            model.addAttribute("previews", previews)
            model.addAttribute("filterDeleted", "Filter removed successfully!")
        }

        return "fragments/tools :: preview"
    }

    @PostMapping("/merge-xml")
    fun mergeXml(
        @RequestParam(value = "excludedIds", required = false) excludedIds: List<String>?,
        session: HttpSession,
    ): ResponseEntity<ByteArray> {
        @Suppress("UNCHECKED_CAST")
        val documents = session.getAttribute("mergePreviewDocs") as? List<MxCamt05300102>

        if (documents.isNullOrEmpty()) {
            return ResponseEntity.badRequest().build()
        }

        val merged = camtMergerService.mergeFromDocuments(documents, excludedIds ?: emptyList())

        // Clean up session
        session.removeAttribute("mergePreviewDocs")

        return ResponseEntity
            .ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"merged-camt.xml\"")
            .contentType(MediaType.APPLICATION_XML)
            .body(merged)
    }
}

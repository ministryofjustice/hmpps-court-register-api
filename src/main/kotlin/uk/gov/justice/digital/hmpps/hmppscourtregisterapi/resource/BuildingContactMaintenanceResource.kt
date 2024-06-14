package uk.gov.justice.digital.hmpps.hmppscourtregisterapi.resource

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.service.AuditService
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.service.AuditType
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.service.BuildingContactService
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.service.EventType
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.service.SnsService

@RestController
@Validated
@RequestMapping(name = "Court Maintenance", path = ["/court-maintenance"], produces = [MediaType.APPLICATION_JSON_VALUE])
class BuildingContactMaintenanceResource(
  private val contactService: BuildingContactService,
  private val snsService: SnsService,
  private val auditService: AuditService,
) {
  @PreAuthorize("hasRole('ROLE_MAINTAIN_REF_DATA') and hasAuthority('SCOPE_write')")
  @Operation(
    summary = "Update specified building contact details",
    description = "Updates contact information, role required is MAINTAIN_REF_DATA",
    security = [SecurityRequirement(name = "MAINTAIN_REF_DATA", scopes = ["write"])],
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = UpdateContactDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Building Contact Information Updated",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ContactDto::class))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Information request to update contact",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to make contact update",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Contact ID not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PutMapping("/id/{courtId}/buildings/{buildingId}/contacts/{contactId}")
  fun updateContact(
    @Schema(description = "Court ID", example = "ACCRYC", required = true)
    @PathVariable
    @Size(max = 12, min = 2, message = "Court ID must be between 2 and 12")
    courtId: String,
    @Schema(description = "Building ID", example = "234231", required = true)
    @PathVariable
    buildingId: Int,
    @Schema(description = "Contact ID", example = "11111", required = true)
    @PathVariable
    contactId: Int,
    @RequestBody @Valid
    updateContactDto: UpdateContactDto,
  ): ContactDto {
    val updatedContact = contactService.updateContact(courtId, buildingId, contactId, updateContactDto)
    snsService.sendEvent(EventType.COURT_REGISTER_UPDATE, courtId)
    auditService.sendAuditEvent(
      AuditType.COURT_REGISTER_CONTACT_UPDATE.name,
      mapOf("courtId" to courtId, "buildingId" to buildingId, "contact" to updatedContact),
    )
    return updatedContact
  }

  @PreAuthorize("hasRole('ROLE_MAINTAIN_REF_DATA') and hasAuthority('SCOPE_write')")
  @PostMapping("/id/{courtId}/buildings/{buildingId}/contacts")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Add a new contact to a building",
    description = "Adds a new contact to building, role required is MAINTAIN_REF_DATA",
    security = [SecurityRequirement(name = "MAINTAIN_REF_DATA", scopes = ["write"])],
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = UpdateContactDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Contact Information Inserted",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ContactDto::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid request to add a contact",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to add contact insert",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun insertContact(
    @Schema(description = "Court ID", example = "ACCRYC", required = true)
    @PathVariable
    @Size(max = 12, min = 2, message = "Court ID must be between 2 and 12")
    courtId: String,
    @Schema(description = "Contact ID", example = "11111", required = true)
    @PathVariable
    buildingId: Int,
    @RequestBody @Valid
    updateContactDto: UpdateContactDto,
  ): ContactDto {
    val newContact = contactService.insertContact(courtId, buildingId, updateContactDto)
    snsService.sendEvent(EventType.COURT_REGISTER_UPDATE, courtId)
    auditService.sendAuditEvent(
      AuditType.COURT_REGISTER_CONTACT_INSERT.name,
      mapOf("courtId" to courtId, "buildingId" to buildingId, "contact" to newContact),
    )

    return newContact
  }

  @PreAuthorize("hasRole('ROLE_MAINTAIN_REF_DATA') and hasAuthority('SCOPE_write')")
  @Operation(
    summary = "Delete specified building contact details",
    description = "Deletes contact information, role required is MAINTAIN_REF_DATA",
    security = [SecurityRequirement(name = "MAINTAIN_REF_DATA", scopes = ["write"])],
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Building Contact Information Deleted",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ContactDto::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to delete contact",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Contact ID not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @DeleteMapping("/id/{courtId}/buildings/{buildingId}/contacts/{contactId}")
  fun deleteContact(
    @Schema(description = "Court ID", example = "ACCRYC", required = true)
    @PathVariable
    @Size(max = 12, min = 2, message = "Court ID must be between 2 and 12")
    courtId: String,
    @Schema(description = "Building ID", example = "234231", required = true)
    @PathVariable
    buildingId: Int,
    @Schema(description = "Contact ID", example = "11111", required = true)
    @PathVariable
    contactId: Int,
  ) {
    contactService.deleteContact(courtId, buildingId, contactId)
    snsService.sendEvent(EventType.COURT_REGISTER_UPDATE, courtId)
    auditService.sendAuditEvent(
      AuditType.COURT_REGISTER_CONTACT_DELETE.name,
      mapOf("courtId" to courtId, "buildingId" to buildingId, "contactId" to contactId),
    )
  }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Contact")
data class UpdateContactDto(
  @Schema(description = "Type of contact", example = "TEL", required = true, allowableValues = [ "TEL", "FAX"]) val type: String,
  @Schema(description = "Details of the contact", example = "555 55555", required = true)
  @field:Size(
    max = 80,
    message = "Details must be no more than 80 characters",
  )
  val detail: String,
)

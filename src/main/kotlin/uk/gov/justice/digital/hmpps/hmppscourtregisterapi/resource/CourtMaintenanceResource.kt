package uk.gov.justice.digital.hmpps.hmppscourtregisterapi.resource

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
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
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.service.CourtService
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.service.EventType.COURT_REGISTER_UPDATE
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.service.SnsService

@RestController
@Validated
@RequestMapping(name = "Court Maintenance", path = ["/court-maintenance"], produces = [MediaType.APPLICATION_JSON_VALUE])
class CourtMaintenanceResource(
  private val courtService: CourtService,
  private val snsService: SnsService,
  private val auditService: AuditService,
) {
  @PreAuthorize("hasRole('ROLE_MAINTAIN_REF_DATA') and hasAuthority('SCOPE_write')")
  @Operation(
    summary = "Update specified court details",
    description = "Updates court information, role required is MAINTAIN_REF_DATA",
    security = [SecurityRequirement(name = "MAINTAIN_REF_DATA", scopes = ["write"])],
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = UpdateCourtDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Court Information Updated",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = CourtDto::class))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Information request to update court",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to make court update",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Court ID not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PutMapping("/id/{courtId}")
  fun updateCourt(
    @Schema(description = "Court ID", example = "ACCRYC", required = true)
    @PathVariable
    @Size(max = 12, min = 2, message = "Court ID must be between 2 and 12")
    courtId: String,
    @RequestBody @Valid
    courtUpdateRecord: UpdateCourtDto,
  ): CourtDto {
    val updatedCourt = courtService.updateCourt(courtId, courtUpdateRecord)
    snsService.sendEvent(COURT_REGISTER_UPDATE, courtId)
    auditService.sendAuditEvent(
      AuditType.COURT_REGISTER_UPDATE.name,
      courtId to courtUpdateRecord,
    )
    return updatedCourt
  }

  @PreAuthorize("hasRole('ROLE_MAINTAIN_REF_DATA') and hasAuthority('SCOPE_write')")
  @PostMapping("")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Add a new court",
    description = "Adds a new court information, role required is MAINTAIN_REF_DATA",
    security = [SecurityRequirement(name = "MAINTAIN_REF_DATA", scopes = ["write"])],
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = InsertCourtDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Court Information Inserted",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = CourtDto::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Information request to add a court",
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
        description = "Incorrect permissions to make court insert",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun insertCourt(
    @RequestBody @Valid
    courtInsertRecord: InsertCourtDto,
  ): CourtDto {
    val newCourt = courtService.findById(courtService.insertCourt(courtInsertRecord))
    snsService.sendEvent(COURT_REGISTER_UPDATE, newCourt.courtId)
    auditService.sendAuditEvent(
      AuditType.COURT_REGISTER_INSERT.name,
      courtInsertRecord,
    )

    return newCourt
  }
}

@JsonInclude(NON_NULL)
@Schema(description = "Court Insert Record")
data class InsertCourtDto(
  @Schema(description = "Court ID", example = "ACCRYC", required = true)
  @field:Size(max = 6, min = 2, message = "Court ID must be between 2 and 6")
  @NotBlank
  val courtId: String,
  @Schema(description = "Name of the court", example = "Accrington Youth Court", required = true)
  @field:Size(
    max = 80,
    min = 2,
    message = "Court name must be between 2 and 80",
  )
  @field:NotBlank(message = "Court ID is required")
  val courtName: String,
  @Schema(description = "Description of the court", example = "Accrington Youth Court", required = false)
  @field:Size(
    max = 200,
    min = 2,
    message = "Court name must be between 2 and 200",
  )
  val courtDescription: String?,
  @Schema(description = "Type of court", example = "COU", required = true) val courtType: String,
  @Schema(description = "Whether the court is still active", required = true) val active: Boolean,
  @Schema(description = "List of buildings for this court") val buildings: List<UpdateBuildingDto> = listOf(),
)

@JsonInclude(NON_NULL)
@Schema(description = "Court Update Record")
data class UpdateCourtDto(
  @Schema(description = "Name of the court", example = "Accrington Youth Court", required = true)
  @field:Size(
    max = 80,
    min = 2,
    message = "Court name must be between 2 and 80",
  )
  @field:NotBlank(message = "Court ID is required")
  val courtName: String,
  @Schema(description = "Description of the court", example = "Accrington Youth Court", required = false)
  @field:Size(
    max = 200,
    min = 2,
    message = "Court name must be between 2 and 200",
  )
  val courtDescription: String?,
  @Schema(description = "Type of court", example = "COU", required = true) val courtType: String,
  @Schema(description = "Whether the court is still active", required = true) val active: Boolean,
)

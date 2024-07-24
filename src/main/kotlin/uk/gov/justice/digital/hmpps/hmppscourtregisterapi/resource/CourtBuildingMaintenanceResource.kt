package uk.gov.justice.digital.hmpps.hmppscourtregisterapi.resource

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
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
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.service.CourtBuildingService
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.service.EventType
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.service.SnsService

@RestController
@Validated
@RequestMapping(name = "Court Maintenance", path = ["/court-maintenance"], produces = [MediaType.APPLICATION_JSON_VALUE])
class CourtBuildingMaintenanceResource(
  private val buildingService: CourtBuildingService,
  private val snsService: SnsService,
  private val auditService: AuditService,
) {
  @PreAuthorize("hasRole('ROLE_COURT_REGISTER__COURT_DETAILS__RW')")
  @Operation(
    summary = "Update specified building details",
    description = "Updates building information, role required is ROLE_COURT_REGISTER__COURT_DETAILS__RW",
    security = [SecurityRequirement(name = "ROLE_COURT_REGISTER__COURT_DETAILS__RW")],
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = UpdateBuildingDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Building Information Updated",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = BuildingDto::class))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Information request to update building",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to make building update",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Building ID not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PutMapping("/id/{courtId}/buildings/{buildingId}")
  fun updateBuilding(
    @Schema(description = "Court ID", example = "ACCRYC", required = true)
    @PathVariable
    @Size(max = 12, min = 2, message = "Court ID must be between 2 and 12")
    courtId: String,
    @Schema(description = "Building ID", example = "234231", required = true)
    @PathVariable
    buildingId: Int,
    @RequestBody @Valid
    updateBuildingDto: UpdateBuildingDto,
  ): BuildingDto {
    val updatedBuilding = buildingService.updateBuilding(courtId, buildingId, updateBuildingDto)
    snsService.sendEvent(EventType.COURT_REGISTER_UPDATE, courtId)
    auditService.sendAuditEvent(
      AuditType.COURT_REGISTER_BUILDING_UPDATE.name,
      mapOf("courtId" to courtId, "buildingId" to buildingId, "building" to updatedBuilding),
    )
    return updatedBuilding
  }

  @PreAuthorize("hasRole('ROLE_COURT_REGISTER__COURT_DETAILS__RW')")
  @PostMapping("/id/{courtId}/buildings")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Add a new building to a court",
    description = "Adds a new building to court, role required is ROLE_COURT_REGISTER__COURT_DETAILS__RW",
    security = [SecurityRequirement(name = "ROLE_COURT_REGISTER__COURT_DETAILS__RW")],
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = UpdateBuildingDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Building Information Inserted",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = BuildingDto::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid request to add a building",
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
        description = "Incorrect permissions to make building insert",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun insertBuilding(
    @Schema(description = "Court ID", example = "ACCRYC", required = true)
    @PathVariable
    @Size(max = 12, min = 2, message = "Court ID must be between 2 and 12")
    courtId: String,
    @RequestBody @Valid
    updateBuildingDto: UpdateBuildingDto,
  ): BuildingDto {
    val newBuilding = buildingService.insertBuilding(courtId, updateBuildingDto)
    snsService.sendEvent(EventType.COURT_REGISTER_UPDATE, courtId)
    auditService.sendAuditEvent(
      AuditType.COURT_REGISTER_BUILDING_INSERT.name,
      mapOf("courtId" to courtId, "building" to newBuilding),
    )

    return newBuilding
  }

  @PreAuthorize("hasRole('ROLE_COURT_REGISTER__COURT_DETAILS__RW')")
  @Operation(
    summary = "Delete specified building",
    description = "Deletes building information, role required is ROLE_COURT_REGISTER__COURT_DETAILS__RW",
    security = [SecurityRequirement(name = "ROLE_COURT_REGISTER__COURT_DETAILS__RW")],
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Building Information Deleted",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = BuildingDto::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to delete building",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Building ID not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @DeleteMapping("/id/{courtId}/buildings/{buildingId}")
  fun deleteBuilding(
    @Schema(description = "Court ID", example = "ACCRYC", required = true)
    @PathVariable
    @Size(max = 12, min = 2, message = "Court ID must be between 2 and 12")
    courtId: String,
    @Schema(description = "Building ID", example = "234231", required = true)
    @PathVariable
    buildingId: Int,
  ) {
    buildingService.deleteBuilding(courtId, buildingId)
    snsService.sendEvent(EventType.COURT_REGISTER_UPDATE, courtId)
    auditService.sendAuditEvent(
      AuditType.COURT_REGISTER_BUILDING_DELETE.name,
      mapOf("courtId" to courtId, "buildingId" to buildingId),
    )
  }
}

@JsonInclude(NON_NULL)
@Schema(description = "Building Update Record")
data class UpdateBuildingDto(
  @Schema(description = "Address Line 1", example = "Crown House")
  @field:Size(
    max = 50,
    message = "Address Line 1 must be no more than 50 characters",
  )
  val addressLine1: String?,
  @Schema(description = "Address Line 2", example = "452 West Street")
  @field:Size(
    max = 80,
    message = "Address Line 2 must be no more than 80 characters",
  )
  val addressLine2: String?,
  @Schema(description = "Address Line 3", example = "Swansea")
  @field:Size(
    max = 80,
    message = "Address Line 3 must be no more than 80 characters",
  )
  val addressLine3: String?,
  @Schema(description = "Address Line 4", example = "West Cross")
  @field:Size(
    max = 80,
    message = "Address Line 4 must be no more than 80 characters",
  )
  val addressLine4: String?,
  @Schema(description = "Address Line 5", example = "South Glamorgan")
  @field:Size(
    max = 80,
    message = "Address Line 5 must be no more than 80 characters",
  )
  val addressLine5: String?,
  @Schema(description = "Postcode", example = "SA3 4HT")
  @field:Size(
    max = 8,
    message = "Postcode must be no more than 8 characters",
  )
  val postcode: String?,
  @Schema(description = "Sub location code for referencing building", example = "AAABBB")
  @field:Size(
    max = 6,
    message = "Sub location code must be no more than 6 characters",
  )
  val subCode: String?,
  @Schema(description = "Whether the building is active", example = "true", required = false, defaultValue = "true") val active: Boolean = true,
  @Schema(description = "List of contacts for this building by type, can only be used on a new court") val contacts: List<UpdateContactDto> = listOf(),
)

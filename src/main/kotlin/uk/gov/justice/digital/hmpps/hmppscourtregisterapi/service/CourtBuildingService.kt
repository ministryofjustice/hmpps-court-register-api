package uk.gov.justice.digital.hmpps.hmppscourtregisterapi.service

import jakarta.persistence.EntityExistsException
import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa.Building
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa.BuildingRepository
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa.CourtRepository
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.resource.BuildingDto
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.resource.UpdateBuildingDto

@Service
@Transactional
class CourtBuildingService(
  private val courtRepository: CourtRepository,
  private val buildingRepository: BuildingRepository,
) {
  fun findById(courtId: String, buildingId: Int): BuildingDto {
    return BuildingDto(getBuilding(buildingId, courtId))
  }

  fun findBySubCode(subCode: String): BuildingDto {
    return BuildingDto(buildingRepository.findBySubCode(subCode).orElseThrow { EntityNotFoundException("Building subcode $subCode not found") })
  }

  fun findMainBuilding(courtId: String): BuildingDto {
    return BuildingDto(buildingRepository.findMainBuilding(courtId).orElseThrow { EntityNotFoundException("Main building for court id $courtId not found") })
  }

  fun updateBuilding(courtId: String, buildingId: Int, updateBuildingRecord: UpdateBuildingDto): BuildingDto {
    val building = getBuilding(buildingId, courtId)

    if (updateBuildingRecord.subCode != null) {
      buildingRepository.findBySubCode(subCode = updateBuildingRecord.subCode)
        .ifPresent {
          if (buildingId != it.id) {
            throw EntityExistsException("Building with this sub-code already exists")
          }
        }
    }

    with(updateBuildingRecord) {
      building.buildingName = buildingName
      building.street = street
      building.locality = locality
      building.town = town
      building.postcode = postcode
      building.county = county
      building.country = country
      building.subCode = subCode
      building.active = active
    }
    return BuildingDto(building)
  }

  private fun getBuilding(
    buildingId: Int,
    courtId: String,
  ): Building {
    val building = buildingRepository.findById(buildingId)
      .orElseThrow { EntityNotFoundException("Building $buildingId not found") }

    if (courtId != building.court.id) {
      throw EntityNotFoundException("Building $buildingId not in court $courtId")
    }
    return building
  }

  fun insertBuilding(courtId: String, updateBuildingRecord: UpdateBuildingDto): BuildingDto {
    val court = courtRepository.findById(courtId).orElseThrow { EntityNotFoundException("Court $courtId not found") }

    with(updateBuildingRecord) {
      val building = Building(
        court = court,
        subCode = subCode,
        buildingName = buildingName,
        street = street,
        locality = locality,
        town = town,
        postcode = postcode,
        county = county,
        country = country,
        active = active,
      )

      return BuildingDto(buildingRepository.save(building))
    }
  }

  fun deleteBuilding(courtId: String, buildingId: Int) {
    val building = getBuilding(buildingId, courtId)

    buildingRepository.delete(building)
  }
}

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
  fun findById(courtId: String, buildingId: Int): BuildingDto = BuildingDto(getBuilding(buildingId, courtId))

  fun findBySubCode(subCode: String): BuildingDto = BuildingDto(buildingRepository.findBySubCode(subCode).orElseThrow { EntityNotFoundException("Building subcode $subCode not found") })

  fun findMainBuilding(courtId: String): BuildingDto = BuildingDto(buildingRepository.findMainBuilding(courtId).orElseThrow { EntityNotFoundException("Main building for court id $courtId not found") })

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
      building.addressLine1 = addressLine1
      building.addressLine2 = addressLine2
      building.addressLine3 = addressLine3
      building.addressLine4 = addressLine4
      building.postcode = postcode
      building.addressLine5 = addressLine5
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
        addressLine1 = addressLine1,
        addressLine2 = addressLine2,
        addressLine3 = addressLine3,
        addressLine4 = addressLine4,
        postcode = postcode,
        addressLine5 = addressLine5,
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

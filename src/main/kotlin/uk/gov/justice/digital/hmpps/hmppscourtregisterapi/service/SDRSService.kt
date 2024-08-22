package uk.gov.justice.digital.hmpps.hmppscourtregisterapi.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.client.sdrs.SDRSApiClient
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.client.sdrs.dto.SDRSRequest
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa.Building
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa.BuildingRepository
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa.Contact
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa.ContactRepository
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa.Court
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa.CourtRepository
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa.CourtType
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa.CourtTypeRepository

@Service
class SDRSService(private val sdrsApiClient: SDRSApiClient, private val courtTypeRepository: CourtTypeRepository, private val courtRepository: CourtRepository, private val buildingRepository: BuildingRepository, private val contactRepository: ContactRepository) {

  fun refreshData() {
    val courtData = sdrsApiClient.callSDRS(SDRSRequest())
    courtData.messageBody.gatewayOperationType.organisationUnitResponse.organisationUnit.forEach { organisationUnit ->
      val courtType = CourtType.from(organisationUnit.oUCodeL1Name)
      val savedCourtType = courtTypeRepository.findByIdOrNull(courtType.id) ?: courtTypeRepository.save(courtType)
      val court = courtRepository.findByIdOrNull(organisationUnit.oUCode)?.let { court ->
        court.updateFromOrganisationUnit(organisationUnit, savedCourtType)
        courtRepository.save(court)
      } ?: Court.from(organisationUnit, savedCourtType)

      court.courtType = savedCourtType
      val savedCourt = courtRepository.save(court)
      val building = court.buildings.firstOrNull { building -> building.postcode?.equals(organisationUnit.postCode) == true }?.let { building ->
        building.updateFromOrganisationUnit(organisationUnit)
        buildingRepository.save(building)
      } ?: buildingRepository.save(Building.from(organisationUnit, savedCourt))

      organisationUnit.phoneNumber?.let { phoneNumber ->
        building.contacts.firstOrNull { contact -> contact.detail.equals(phoneNumber) }?.let { contact ->
          contact.type = "TEL"
          contact.detail = phoneNumber
          contactRepository.save(contact)
        } ?: contactRepository.save(Contact(building = building, type = "TEL", detail = phoneNumber))
      }
    }
  }
}

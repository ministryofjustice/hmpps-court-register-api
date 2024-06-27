package uk.gov.justice.digital.hmpps.hmppscourtregisterapi.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.client.SDRSApiClient
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.client.dto.SDRSRequest
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa.Building
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa.BuildingRepository
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa.Contact
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa.ContactRepository
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa.Court
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa.CourtRepository
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa.CourtTypeRepository

@Service
class SDRSService(private val sdrsApiClient: SDRSApiClient, private val courtTypeRepository: CourtTypeRepository, private val courtRepository: CourtRepository, private val buildingRepository: BuildingRepository, private val contactRepository: ContactRepository) {

  fun refreshData() {
    val courtData = sdrsApiClient.callSDRS(SDRSRequest())
    courtData.messageBody.gatewayOperationType.organisationUnitResponse.organisationUnit.forEach {
      val court = Court.from(it)
      court.courtType = courtTypeRepository.findByIdOrNull(court.courtType.id) ?: courtTypeRepository.save(court.courtType)
      val savedCourt = courtRepository.save(court)
      val building = buildingRepository.save(Building.from(it, savedCourt))
      it.phoneNumber?.let { phoneNumber -> contactRepository.save(Contact(building = building, type = "TEL", detail = phoneNumber)) }
    }
  }
}

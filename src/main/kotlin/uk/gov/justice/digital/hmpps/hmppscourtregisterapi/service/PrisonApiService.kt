package uk.gov.justice.digital.hmpps.hmppscourtregisterapi.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.client.prisonapi.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.client.prisonapi.dto.Agency
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa.Court
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa.CourtRepository
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa.CourtType
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa.CourtTypeRepository

@Service
class PrisonApiService(private val prisonApiClient: PrisonApiClient, private val courtTypeRepository: CourtTypeRepository, private val courtRepository: CourtRepository) {

  fun refreshData() {
    val courtData = prisonApiClient.getCourtAgencies()
    courtData.forEach { agency: Agency ->
      val courtType = CourtType.from(agency)
      val savedCourtType = courtTypeRepository.findByIdOrNull(courtType.id) ?: courtTypeRepository.save(courtType)
      courtRepository.findByIdOrNull(agency.agencyId)?.let { court ->
        court.updateFromAgency(agency, savedCourtType)
        courtRepository.save(court)
      } ?: courtRepository.save(Court.from(agency, savedCourtType))
    }
  }
}

package uk.gov.justice.digital.hmpps.hmppscourtregisterapi.service

import jakarta.persistence.EntityExistsException
import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa.Court
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa.CourtRepository
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa.CourtTypeRepository
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.resource.CourtDto
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.resource.CourtTypeDto
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.resource.InsertCourtDto
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.resource.UpdateCourtDto

@Service
@Transactional
class CourtService(
  private val courtRepository: CourtRepository,
  private val courtTypeRepository: CourtTypeRepository,
) {
  fun findById(courtId: String): CourtDto {
    val court = courtRepository.findById(courtId)
      .orElseThrow { EntityNotFoundException("Court $courtId not found") }
    return CourtDto(court)
  }

  fun findByIds(courtIds: List<String>): List<CourtDto> = courtRepository.findAllById(courtIds).map { CourtDto(it) }

  fun findAll(activeOnly: Boolean = false): List<CourtDto> {
    if (activeOnly) {
      return courtRepository.findByActiveOrderById(true).map { CourtDto(it) }
    }
    return courtRepository.findAll().map { CourtDto(it) }
  }

  fun findPage(
    active: Boolean? = null,
    courtTypeIds: List<String>? = null,
    textSearch: String? = null,
    pageable: Pageable = Pageable.unpaged(),
  ): Page<CourtDto> = (
    textSearch
      ?.let { courtRepository.findPageWithTextSearch(active, courtTypeIds, textSearch, pageable) }
      ?: courtRepository.findPage(active, courtTypeIds, pageable)
    ).map { CourtDto(it) }

  fun updateCourt(courtId: String, courtUpdateRecord: UpdateCourtDto): CourtDto {
    val court = courtRepository.findById(courtId)
      .orElseThrow { EntityNotFoundException("Court $courtId not found") }

    with(courtUpdateRecord) {
      court.courtName = courtName
      court.courtDescription = courtDescription
      court.courtType = courtTypeRepository.findById(courtType).orElseThrow()
      court.active = active
    }
    return CourtDto(court)
  }

  fun insertCourt(courtInsertRecord: InsertCourtDto): String {
    if (courtRepository.findById(courtInsertRecord.courtId).isPresent) {
      throw EntityExistsException("Court $courtInsertRecord.courtId already exists")
    }

    with(courtInsertRecord) {
      val court = Court(courtId, courtName, courtDescription, courtTypeRepository.findById(courtType).orElseThrow { EntityNotFoundException("Court Type $courtType not found") }, active)
      buildings.forEach {
        court.addBuilding(it)
      }
      return courtRepository.save(court).id
    }
  }

  fun getCourtTypes(): List<CourtTypeDto> = courtTypeRepository.findAll().map { CourtTypeDto(it) }
}

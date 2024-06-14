package uk.gov.justice.digital.hmpps.hmppscourtregisterapi.service

import jakarta.persistence.EntityExistsException
import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa.Court
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa.CourtRepository
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa.CourtType
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa.CourtTypeRepository
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.resource.CourtDto
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.resource.CourtTypeDto
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.resource.InsertCourtDto
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.resource.UpdateCourtDto
import java.util.Optional

class CourtServiceTest {
  private val courtRepository: CourtRepository = mock()
  private val courtTypeRepository: CourtTypeRepository = mock()
  private val courtService = CourtService(courtRepository, courtTypeRepository)

  @Suppress("ClassName")
  @Nested
  inner class findById {
    @Test
    fun `find court`() {
      whenever(courtRepository.findById(anyString())).thenReturn(
        Optional.of(Court("ACCRYC", "A Court", null, CourtType("CROWN", "Crown Court"), true)),
      )
      val courtDto = courtService.findById("ACCRYC")
      assertThat(courtDto).isEqualTo(CourtDto("ACCRYC", "A Court", null, CourtTypeDto("CROWN", "Crown Court"), true))
      verify(courtRepository).findById("ACCRYC")
    }
  }

  @Suppress("ClassName")
  @Nested
  inner class findCourts {
    @Test
    fun `find all active courts`() {
      val listOfCourts = listOf(
        Court("ACCRYC", "A Court 1", null, CourtType("CROWN", "Crown Court"), true),
        Court("ACCRYV", "A Court 2", null, CourtType("COUNTY", "County Court"), true),
        Court("ACCRYT", "A Court 3", null, CourtType("OTHER", "Other Type Court"), true),
      )
      whenever(courtRepository.findByActiveOrderById(true)).thenReturn(
        listOfCourts,
      )
      val courts = courtService.findAll(true)
      assertThat(courts).isEqualTo(
        listOf(
          CourtDto("ACCRYC", "A Court 1", null, CourtTypeDto("CROWN", "Crown Court"), true),
          CourtDto("ACCRYV", "A Court 2", null, CourtTypeDto("COUNTY", "County Court"), true),
          CourtDto("ACCRYT", "A Court 3", null, CourtTypeDto("OTHER", "Other Type Court"), true),
        ),
      )
      verify(courtRepository).findByActiveOrderById(true)
    }

    @Test
    fun `find all courts`() {
      val listOfCourts = listOf(
        Court("ACCRYC", "A Court 1", null, CourtType("CROWN", "Crown Court"), true),
        Court("ACCRYV", "A Court 2", null, CourtType("COUNTY", "County Court"), true),
        Court("ACCRYT", "A Court 3", null, CourtType("OTHER", "Other Type Court"), false),
      )
      whenever(courtRepository.findAll()).thenReturn(
        listOfCourts,
      )
      val courts = courtService.findAll()
      assertThat(courts).isEqualTo(
        listOf(
          CourtDto("ACCRYC", "A Court 1", null, CourtTypeDto("CROWN", "Crown Court"), true),
          CourtDto("ACCRYV", "A Court 2", null, CourtTypeDto("COUNTY", "County Court"), true),
          CourtDto("ACCRYT", "A Court 3", null, CourtTypeDto("OTHER", "Other Type Court"), false),
        ),
      )
      verify(courtRepository).findAll()
    }
  }

  @Suppress("ClassName")
  @Nested
  inner class maintainCourts {
    @Test
    fun `update a court`() {
      whenever(courtRepository.findById("ACCRYC")).thenReturn(
        Optional.of(Court("ACCRYC", "A Court 1", null, CourtType("CROWN", "Crown Court"), true)),
      )
      whenever(courtTypeRepository.findById("CROWN")).thenReturn(Optional.of(CourtType("CROWN", "Crown Court")))
      val updatedCourt =
        courtService.updateCourt("ACCRYC", UpdateCourtDto("A Court 1", "add description", "CROWN", true))
      assertThat(updatedCourt).isEqualTo(
        CourtDto("ACCRYC", "A Court 1", "add description", CourtTypeDto("CROWN", "Crown Court"), true),
      )
      verify(courtRepository).findById("ACCRYC")
    }

    @Test
    fun `try to update a court that doesn't exist`() {
      whenever(courtRepository.findById("ACCRYC")).thenReturn(
        Optional.empty(),
      )
      assertThrows(EntityNotFoundException::class.java) {
        courtService.updateCourt("ACCRYC", UpdateCourtDto("A Court 1", "add description", "CROWN", true))
      }

      verify(courtRepository).findById("ACCRYC")
    }

    @Test
    fun `create a court`() {
      val courtToSave = Court("ACCRYZ", "A Court 4", "new court", CourtType("CROWN", "Crown Court"), true)
      whenever(courtRepository.findById("ACCRYZ")).thenReturn(
        Optional.empty(),
        Optional.of(courtToSave),
      )
      whenever(courtRepository.save(courtToSave)).thenReturn(
        courtToSave,
      )
      whenever(courtTypeRepository.findById("CROWN")).thenReturn(Optional.of(CourtType("CROWN", "Crown Court")))
      val courtInsertRecord = InsertCourtDto("ACCRYZ", "A Court 4", "new court", "CROWN", true)
      val updatedCourt = courtService.insertCourt(courtInsertRecord)
      assertThat(updatedCourt).isEqualTo("ACCRYZ")
      verify(courtRepository).findById("ACCRYZ")
      verify(courtTypeRepository).findById("CROWN")
    }

    @Test
    fun `try to create a court that already exists`() {
      whenever(courtRepository.findById("ACCRYZ")).thenReturn(
        Optional.of(Court("ACCRYZ", "A Court 5", "new court 5", CourtType("CROWN", "Crown Court"), false)),
      )
      val courtInsertRecord = InsertCourtDto("ACCRYZ", "A Court 4", "new court", "CROWN", true)
      assertThrows(EntityExistsException::class.java) { courtService.insertCourt(courtInsertRecord) }

      verify(courtRepository).findById("ACCRYZ")
    }
  }
}

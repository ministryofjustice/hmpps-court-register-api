package uk.gov.justice.digital.hmpps.hmppscourtregisterapi.resource

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa.Court
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa.CourtRepository
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa.CourtType

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
class CourtResourceMultipleTest : IntegrationTestBase() {

  @Autowired
  private lateinit var courtRepository: CourtRepository

  private val testCourts = listOf(
    Court("AAAAAA", "AAAAAA New Court", "a description", CourtType("YTH", "Youth Court"), false),
    Court("AAAAAB", "AAAAAB New Court", "a description", CourtType("COU", "County Court"), true),
    Court("AAAAAC", "AAAAAC New Court", "a description", CourtType("CRN", "Crown Court"), true),
  )

  @BeforeAll
  fun `insert inactive test court`() {
    testCourts.forEach {
      courtRepository.save(it)
    }
  }

  @AfterAll
  fun `remove inactive test court`() {
    testCourts.forEach {
      courtRepository.delete(it)
    }
  }

  @Test
  fun `find multiple courts by ids`() {
    webTestClient.get().uri("/courts/id/multiple?courtIds=AAAAAA,AAAAAB")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.length()").isEqualTo(2)
      .jsonPath("$.[0].courtId").isEqualTo("AAAAAA")
  }

  @Test
  fun `not supplying courtIds results in a bad request`() {
    webTestClient.get().uri("/courts/id/multiple")
      .exchange()
      .expectStatus().isBadRequest
  }
}

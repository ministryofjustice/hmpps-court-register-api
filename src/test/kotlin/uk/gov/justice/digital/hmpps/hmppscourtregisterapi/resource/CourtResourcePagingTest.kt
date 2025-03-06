package uk.gov.justice.digital.hmpps.hmppscourtregisterapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa.Court
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa.CourtRepository
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa.CourtType

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
class CourtResourcePagingTest : IntegrationTestBase() {

  companion object {
    val log = LoggerFactory.getLogger(this::class.java)
  }

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
  fun `find page of courts`() {
    webTestClient.get().uri("/courts/paged?page=0&size=3&sort=courtName")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .assertFirstPageOfMany()
      .jsonPath("$.content[0].courtId").isEqualTo("AAAAAA")
      .jsonPath("$.content[0].active").isEqualTo(false)
  }

  private fun WebTestClient.BodyContentSpec.assertFirstPageOfMany() = this.jsonPath("$.content.length()").isEqualTo(3)
    .jsonPath("$.size").isEqualTo(3)
    .jsonPath("$.totalElements").value<Int> { assertThat(it).isGreaterThan(3) }
    .jsonPath("$.totalPages").value<Int> { assertThat(it).isGreaterThan(1) }
    .jsonPath("$.last").isEqualTo(false)

  @Test
  fun `find page of active courts`() {
    webTestClient.get().uri("/courts/paged?page=0&size=3&sort=courtName&active=true")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .assertFirstPageOfMany()
      .jsonPath("$.content[0].courtId").value<String> { assertThat(it).isNotEqualTo("AAAAAA") }
      .jsonPath("$.content[0].active").isEqualTo(true)
  }

  @Test
  fun `find page filtered by court type`() {
    webTestClient.get().uri("/courts/paged?page=0&size=3&sort=courtName&courtTypeIds=YTH")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .assertFirstPageOfMany()
      .jsonPath("$.content[0].courtId").value<String> { assertThat(it).isEqualTo("AAAAAA") }
      .jsonPath("$.content[0].type.courtType").isEqualTo("YTH")
      .jsonPath("$.content[1].type.courtType").isEqualTo("YTH")
      .jsonPath("$.content[2].type.courtType").isEqualTo("YTH")
  }

  @Test
  fun `find page filtered by court type and active flag`() {
    webTestClient.get().uri("/courts/paged?page=0&size=3&sort=courtName&courtTypeIds=YTH&active=true")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .assertFirstPageOfMany()
      .jsonPath("$.content[0].courtId").value<String> { assertThat(it).isNotEqualTo("AAAAAA") }
      .jsonPath("$.content[0].type.courtType").isEqualTo("YTH")
      .jsonPath("$.content[0].active").isEqualTo(true)
      .jsonPath("$.content[1].type.courtType").isEqualTo("YTH")
      .jsonPath("$.content[1].active").isEqualTo(true)
      .jsonPath("$.content[2].type.courtType").isEqualTo("YTH")
      .jsonPath("$.content[2].active").isEqualTo(true)
  }

  @Test
  fun `find page filtered by multiple court types`() {
    webTestClient.get().uri("/courts/paged?page=0&size=3&sort=courtName&courtTypeIds=YTH&courtTypeIds=CRN")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .assertFirstPageOfMany()
      .jsonPath("$.content[0].courtId").value<String> { assertThat(it).isEqualTo("AAAAAA") }
      .jsonPath("$.content[1].courtId").value<String> { assertThat(it).isEqualTo("AAAAAC") }
      .jsonPath("$.content[2].type.courtType").value<String> { assertThat(it).isIn("YTH", "CRN") }
  }

  @Test
  fun `find page filtered by multiple court types and active flag`() {
    webTestClient.get().uri("/courts/paged?page=0&size=3&sort=courtName&courtTypeIds=YTH&courtTypeIds=CRN&active=true")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .assertFirstPageOfMany()
      .jsonPath("$.content[0].courtId").value<String> { assertThat(it).isEqualTo("AAAAAC") }
      .jsonPath("$.content[1].type.courtType").value<String> { assertThat(it).isIn("YTH", "CRN") }
      .jsonPath("$.content[1].active").isEqualTo(true)
      .jsonPath("$.content[2].type.courtType").value<String> { assertThat(it).isIn("YTH", "CRN") }
      .jsonPath("$.content[2].active").isEqualTo(true)
  }

  @Test
  fun `find page filtered by text search`() {
    webTestClient.get().uri("/courts/paged?page=0&size=3&textSearch=description")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .assertSingleFullPage()
      .jsonPath("$.content[0].courtId").isEqualTo("AAAAAA")
      .jsonPath("$.content[0].courtDescription").value<String> { assertThat(it).contains("description") }
      .jsonPath("$.content[1].courtId").isEqualTo("AAAAAB")
      .jsonPath("$.content[1].courtDescription").value<String> { assertThat(it).contains("description") }
      .jsonPath("$.content[2].courtId").isEqualTo("AAAAAC")
      .jsonPath("$.content[2].courtDescription").value<String> { assertThat(it).contains("description") }
  }

  private fun WebTestClient.BodyContentSpec.assertSingleFullPage() = this.jsonPath("$.content.length()").isEqualTo(3)
    .jsonPath("$.size").isEqualTo(3)
    .jsonPath("$.totalElements").isEqualTo(3)
    .jsonPath("$.totalPages").isEqualTo(1)
    .jsonPath("$.last").isEqualTo(true)

  @Test
  fun `find page filtered by text search and active`() {
    webTestClient.get().uri("/courts/paged?page=0&size=3&active=false&textSearch=description")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content[0].courtId").isEqualTo("AAAAAA")
      .jsonPath("$.content[0].courtDescription").value<String> { assertThat(it).contains("description") }
      .jsonPath("$.content[0].active").isEqualTo(false)
  }

  @Test
  fun `find page filtered by text search and active and court type`() {
    webTestClient.get().uri("/courts/paged?page=0&size=3&active=true&courtTypeId=COU&textSearch=description")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content[0].courtId").isEqualTo("AAAAAB")
      .jsonPath("$.content[0].type.courtType").isEqualTo("COU")
      .jsonPath("$.content[0].courtDescription").value<String> { assertThat(it).contains("description") }
      .jsonPath("$.content[0].active").isEqualTo(true)
  }

  @Test
  fun `find later page filtered by text search and active and multiple court types`() {
    webTestClient.get().uri("/courts/paged?page=1&size=3&active=true&courtTypeIds=COU&courtTypeIds=MAG&textSearch=court")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content.length()").isEqualTo(3)
      .jsonPath("$.size").isEqualTo(3)
      .jsonPath("$.totalElements").value<Int> { assertThat(it).isGreaterThan(6) }
      .jsonPath("$.totalPages").value<Int> { assertThat(it).isGreaterThan(2) }
      .jsonPath("$.first").isEqualTo(false)
      .jsonPath("$.last").isEqualTo(false)
  }

  @Test
  fun `find page filtered by text search on post code`() {
    webTestClient.get().uri("/courts/paged?page=0&size=3&textSearch=BB5 2BH")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content[0].buildings[0].postcode").isEqualTo("BB5 2BH")
  }

  @Test
  fun `find page filtered by text search on post code without spaces`() {
    webTestClient.get().uri("/courts/paged?page=0&size=3&textSearch=BB52BH")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content[0].buildings[0].postcode").isEqualTo("BB5 2BH")
  }

  @Test
  fun `find page filtered by text search on telephone number`() {
    webTestClient.get().uri("/courts/paged?page=0&size=3&textSearch=01545 570886")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content[0].buildings[0].contacts[0].type").isEqualTo("TEL")
      .jsonPath("$.content[0].buildings[0].contacts[0].detail").isEqualTo("01545 570886")
  }

  @Test
  fun `find page filtered by text search on telephone number without spaces`() {
    webTestClient.get().uri("/courts/paged?page=0&size=3&textSearch=01545570886")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content[0].buildings[0].contacts[0].type").isEqualTo("TEL")
      .jsonPath("$.content[0].buildings[0].contacts[0].detail").isEqualTo("01545 570886")
  }

  @Test
  fun `a search finding multiple buildings and contacts should only return the court once`() {
    // Preston Crown Court has 2 buildings each with 2 contacts
    webTestClient.get().uri("/courts/paged?page=0&size=3&textSearch=PRESCC")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.totalElements").isEqualTo(1)
      .jsonPath("$.content[0].courtId").isEqualTo("PRESCC")
  }
}

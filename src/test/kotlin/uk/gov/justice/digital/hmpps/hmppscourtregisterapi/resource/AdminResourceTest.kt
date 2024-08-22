package uk.gov.justice.digital.hmpps.hmppscourtregisterapi.resource

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.wiremock.PrisonApiExtension.Companion.prisonApi
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.wiremock.SDRSApiExtension.Companion.sdrsApi

class AdminResourceTest : IntegrationTestBase() {

  @Test
  fun `idempotent should insert all data from SDRS on a data refresh`() {
    sdrsApi.stubGetSingleCourt()
    webTestClient.post().uri("/admin/refresh-data")
      .exchange()
      .expectStatus().isNoContent

    webTestClient.get().uri("/courts/id/B05XX00")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.courtId").isEqualTo("B05XX00")

    webTestClient.post().uri("/admin/refresh-data")
      .exchange()
      .expectStatus().isNoContent

    webTestClient.get().uri("/courts/id/B05XX00")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.courtId").isEqualTo("B05XX00")
  }

  @Test
  fun `idempotent should insert all data from Prison API on a data refresh`() {
    prisonApi.stubGetSingleCourt()
    webTestClient.post().uri("/admin/refresh-nomis-data")
      .exchange()
      .expectStatus().isNoContent

    webTestClient.get().uri("/courts/id/COURT1")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.courtId").isEqualTo("COURT1")

    webTestClient.post().uri("/admin/refresh-nomis-data")
      .exchange()
      .expectStatus().isNoContent

    webTestClient.get().uri("/courts/id/COURT1")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.courtId").isEqualTo("COURT1")
  }
}

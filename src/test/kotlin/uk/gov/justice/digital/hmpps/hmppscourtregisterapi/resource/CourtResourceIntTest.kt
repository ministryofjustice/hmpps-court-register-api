package uk.gov.justice.digital.hmpps.hmppscourtregisterapi.resource

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.integration.IntegrationTestBase

class CourtResourceIntTest : IntegrationTestBase() {

  @Nested
  inner class FindMainBuilding {

    @Test
    fun `should find main building by court ID`() {
      webTestClient.get().uri("/courts/id/BRMNCC/buildings/main")
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("courtId").isEqualTo("BRMNCC")
        .jsonPath("subCode").doesNotExist()
    }

    @Test
    fun `should not find a building by invalid court ID`() {
      webTestClient.get().uri("/courts/id/BAD/buildings/main")
        .exchange()
        .expectStatus().isNotFound
    }
  }
}

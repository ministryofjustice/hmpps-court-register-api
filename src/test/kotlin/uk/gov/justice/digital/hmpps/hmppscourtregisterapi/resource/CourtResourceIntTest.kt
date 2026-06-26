package uk.gov.justice.digital.hmpps.hmppscourtregisterapi.resource

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.integration.IntegrationTestBase

class CourtResourceIntTest : IntegrationTestBase() {

  @Nested
  inner class FindByCpCourtUuid {

    @Test
    fun `should resolve a Common Platform court UUID to the mapped court`() {
      webTestClient.get().uri("/courts/cp/ac2a72fd-1131-3e18-a84f-0dfe82d70d7a")
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("courtId").isEqualTo("AYLSCC")
        .jsonPath("cpCourtUuid").isEqualTo("ac2a72fd-1131-3e18-a84f-0dfe82d70d7a")
    }

    @Test
    fun `should return 404 when the Common Platform court UUID has no confirmed mapping`() {
      webTestClient.get().uri("/courts/cp/00000000-0000-0000-0000-000000000000")
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `should return 400 for a malformed Common Platform court UUID`() {
      webTestClient.get().uri("/courts/cp/not-a-uuid")
        .exchange()
        .expectStatus().isBadRequest
    }
  }

  @Nested
  inner class FindByCpCourtUuids {

    @Test
    fun `should resolve the mapped subset and omit unmapped UUIDs`() {
      webTestClient.get().uri { builder ->
        builder.path("/courts/cp/multiple")
          .queryParam(
            "cpCourtUuids",
            "ac2a72fd-1131-3e18-a84f-0dfe82d70d7a",
            "662d4ac8-ad0e-37dc-b25b-29c411ca3fdc",
            "00000000-0000-0000-0000-000000000000",
          )
          .build()
      }
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.length()").isEqualTo(2)
        .jsonPath("$[?(@.courtId == 'AYLSCC')]").exists()
        .jsonPath("$[?(@.courtId == 'ABRYMC')]").exists()
    }

    @Test
    fun `should return an empty list when none of the UUIDs are mapped`() {
      webTestClient.get().uri { builder ->
        builder.path("/courts/cp/multiple")
          .queryParam("cpCourtUuids", "00000000-0000-0000-0000-000000000000")
          .build()
      }
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.length()").isEqualTo(0)
    }

    @Test
    fun `should return 400 when any UUID is malformed`() {
      webTestClient.get().uri { builder ->
        builder.path("/courts/cp/multiple")
          .queryParam("cpCourtUuids", "ac2a72fd-1131-3e18-a84f-0dfe82d70d7a", "not-a-uuid")
          .build()
      }
        .exchange()
        .expectStatus().isBadRequest
    }
  }

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

package uk.gov.justice.digital.hmpps.hmppscourtregisterapi.resource

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.integration.IntegrationTestBase

@Transactional
class CourtBuildingMaintenanceIntTest : IntegrationTestBase() {
  @Nested
  @DisplayName("Updating court buildings")
  inner class UpdatingBuildings {
    @Test
    fun `can update a building`() {
      webTestClient.put().uri("court-maintenance/id/ABDRCT/buildings/767")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers {
          it.authToken(
            roles = listOf("ROLE_COURT_REGISTER__COURT_DETAILS__RW"),
            user = "bobby.beans",
          )
        }
        .body(
          BodyInserters.fromValue(
            """
              {
                "addressLine1": "New Crown Building",
                "addressLine2": "Green Street",
                "addressLine3": "M GLAM",
                "addressLine5": "Aberdare",
                "postcode": "CF44 7DW",
                "active": false
            }
            """.trimIndent(),
          ),
        )
        .exchange()
        .expectStatus().isOk

      webTestClient.get().uri("/courts/id/ABDRCT/buildings/id/767")
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.addressLine1").isEqualTo("New Crown Building")
        .jsonPath("$.active").isEqualTo(false)
    }

    @Test
    fun `can update a building subcode`() {
      webTestClient.put().uri("court-maintenance/id/ABDRCT/buildings/767")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers {
          it.authToken(
            roles = listOf("ROLE_COURT_REGISTER__COURT_DETAILS__RW"),
            user = "bobby.beans",
          )
        }
        .body(
          BodyInserters.fromValue(
            """
              {
                "addressLine1": "Crown Building",
                "subCode": "ZYXAA",
                "addressLine2": "Green Street",
                "addressLine3": "M GLAM",
                "addressLine5": "Aberdare",
                "postcode": "CF44 7DW"
            }
            """.trimIndent(),
          ),
        )
        .exchange()
        .expectStatus().isOk

      webTestClient.get().uri("/courts/id/ABDRCT/buildings/id/767")
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.subCode").isEqualTo("ZYXAA")
    }

    @Test
    fun `can not update a building sub-code to existing court code`() {
      webTestClient.put().uri("court-maintenance/id/ABDRCT/buildings/767")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers {
          it.authToken(
            roles = listOf("ROLE_COURT_REGISTER__COURT_DETAILS__RW"),
            user = "bobby.beans",
          )
        }
        .body(
          BodyInserters.fromValue(
            """
              {
                "addressLine1": "Crown Building",
                "subCode": "BRMNCC",
                "addressLine2": "Green Street",
                "addressLine3": "M GLAM",
                "addressLine5": "Aberdare",
                "postcode": "CF44 7DW"
            }
            """.trimIndent(),
          ),
        )
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.CONFLICT)
    }

    @Test
    fun `can not update a building sub-code to existing building sub-code`() {
      webTestClient.put().uri("court-maintenance/id/ABDRCT/buildings/767")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers {
          it.authToken(
            roles = listOf("ROLE_COURT_REGISTER__COURT_DETAILS__RW"),
            user = "bobby.beans",
          )
        }
        .body(
          BodyInserters.fromValue(
            """
              {
                "addressLine1": "Crown Building",
                "subCode": "BCCACC",
                "addressLine2": "Green Street",
                "addressLine3": "M GLAM",
                "addressLine5": "Aberdare",
                "postcode": "CF44 7DW"
            }
            """.trimIndent(),
          ),
        )
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.CONFLICT)
    }

    @Test
    fun `can not have identical building subcodes for the same court`() {
      webTestClient.post().uri("court-maintenance/id/BRMNCC/buildings")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers {
          it.authToken(
            roles = listOf("ROLE_COURT_REGISTER__COURT_DETAILS__RW"),
            user = "bobby.beans",
          )
        }
        .body(
          BodyInserters.fromValue(
            """
              {
                "addressLine1": "New Building 1",
                "subCode": "BCCACC",
                "addressLine2": "Green Street",
                "addressLine3": "M GLAM",
                "addressLine5": "Aberdare",
                "postcode": "CF44 7DW"
            }
            """.trimIndent(),
          ),
        )
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.CONFLICT)
    }

    @Test
    fun `can not have 2 null building subcodes for the same court`() {
      webTestClient.post().uri("court-maintenance/id/AYLSYC/buildings")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers {
          it.authToken(
            roles = listOf("ROLE_COURT_REGISTER__COURT_DETAILS__RW"),
            user = "bobby.beans",
          )
        }
        .body(
          BodyInserters.fromValue(
            """
              {
                "addressLine1": "New Building 1",
                "subCode": null,
                "addressLine2": "Green Street",
                "addressLine3": "M GLAM",
                "addressLine5": "Aberdare",
                "postcode": "CF44 7DW"
            }
            """.trimIndent(),
          ),
        )
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.CONFLICT)
    }
  }
}

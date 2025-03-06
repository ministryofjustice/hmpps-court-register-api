package uk.gov.justice.digital.hmpps.hmppscourtregisterapi.resource

import net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.hamcrest.CoreMatchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa.Building
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa.BuildingRepository
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa.Contact
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa.ContactRepository
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa.Court
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa.CourtRepository
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa.CourtType
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa.CourtTypeRepository
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.Optional
import java.util.function.Consumer

@Suppress("ClassName")
class CourtResourceTest : IntegrationTestBase() {
  @MockBean
  private lateinit var courtRepository: CourtRepository

  @MockBean
  private lateinit var buildingRepository: BuildingRepository

  @MockBean
  private lateinit var contactRepository: ContactRepository

  @MockBean
  private lateinit var courtTypeRepository: CourtTypeRepository

  @Nested
  inner class findAll {
    @Test
    fun `find active courts`() {
      val courts = listOf(
        Court("ACCRYC", "Accrington Youth Court", null, CourtType("YOUTH", "Youth Court"), true),
        Court("KIDDYC", "Kidderminster Youth Court", null, CourtType("YOUTH", "Youth Court"), true),
      )

      whenever(courtRepository.findByActiveOrderById(true)).thenReturn(
        courts,
      )
      webTestClient.get().uri("/courts")
        .exchange()
        .expectStatus().isOk
        .expectBody().json("courts".loadJson())
    }

    @Test
    fun `find all courts`() {
      val courts = listOf(
        Court("ACCRYC", "Accrington Youth Court", null, CourtType("YOUTH", "Youth Court"), true),
        Court("KIDDYC", "Kidderminster Youth Court", null, CourtType("YOUTH", "Youth Court"), true),
        Court("KIDDYE", "Kidderminster Crown Court", null, CourtType("CROWN", "Crown Court"), false),
      )

      whenever(courtRepository.findAll()).thenReturn(
        courts,
      )
      webTestClient.get().uri("/courts/all")
        .exchange()
        .expectStatus().isOk
        .expectBody().json("courts_all".loadJson())
    }

    @Test
    fun `find courts types`() {
      whenever(courtTypeRepository.findAll()).thenReturn(
        listOf(
          CourtType("CROWN", "Crown Court"),
          CourtType("YOUTH", "Youth Court"),
        ),
      )

      webTestClient.get().uri("/courts/types")
        .exchange()
        .expectStatus().isOk
        .expectBody().json("court_types".loadJson())
    }
  }

  @Nested
  inner class updateAndInsertCourts {

    @BeforeEach
    internal fun drainAuditQueue() {
      auditQueue.sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(auditQueue.queueUrl).build())
      await untilCallTo { auditQueue.sqsClient.countMessagesOnQueue(auditQueue.queueUrl).get() } matches { it == 0 }
    }

    @Test
    fun `correct permission are needed to update court data`() {
      webTestClient.put()
        .uri("/court-maintenance/id/ACCRYC")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_DUMMY")))
        .body(BodyInserters.fromValue(UpdateCourtDto("Updated Court", "a description", "YOUTH", false)))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `update a court`() {
      whenever(courtRepository.findById("ACCRYC")).thenReturn(
        Optional.of(Court("ACCRYC", "A Court 1", null, CourtType("YOUTH", "Youth Court"), true)),
      )
      whenever(courtTypeRepository.findById("YOUTH")).thenReturn(Optional.of(CourtType("YOUTH", "Youth Court")))
      webTestClient.put()
        .uri("/court-maintenance/id/ACCRYC")
        .accept(MediaType.APPLICATION_JSON)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_COURT_REGISTER__COURT_DETAILS__RW"),
            user = "bobby.beans",
          ),
        )
        .body(BodyInserters.fromValue(UpdateCourtDto("Updated Court", "a description", "YOUTH", false)))
        .exchange()
        .expectStatus().isOk
        .expectBody().json("updated_court".loadJson())

      assertThat(auditEventMessageCount()).isEqualTo(1)
      val auditMessage = auditMessage()
      assertThatJson(auditMessage).node("what").isEqualTo("COURT_REGISTER_UPDATE")
      assertThatJson(auditMessage).node("who").isEqualTo("bobby.beans")
      assertThatJson(auditMessage).node("service").isEqualTo("hmpps-court-register-api")
      assertThatJson(auditMessage).node("details").isNotNull
      assertThatJson(auditMessage).node("when").asString().satisfies(
        Consumer {
          val whenDateTime = LocalDateTime.ofInstant(Instant.parse(it), ZoneOffset.UTC)
          assertThat(whenDateTime).isCloseToUtcNow(within(5, ChronoUnit.SECONDS))
        },
      )
    }

    @Test
    fun `update a court with bad data`() {
      webTestClient.put()
        .uri("/court-maintenance/id/ACCRYC")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_COURT_REGISTER__COURT_DETAILS__RW")))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "courtName" to "A",
              "courtDescription" to "B",
              "courtType" to "DUMMY",
              "active" to "true",
            ),
          ),
        )
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun `insert a court`() {
      val courtType = CourtType("YOUTH", "Youth Court")
      val court = Court("ACCRYD", "A New Court", "a description", courtType, true)
      whenever(courtRepository.findById("ACCRYD")).thenReturn(
        Optional.empty(),
        Optional.of(court),
      )
      whenever(courtTypeRepository.findById("YOUTH")).thenReturn(Optional.of(courtType))

      whenever(courtRepository.save(any())).thenReturn(
        court,
      )
      webTestClient.post()
        .uri("/court-maintenance")
        .accept(MediaType.APPLICATION_JSON)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_COURT_REGISTER__COURT_DETAILS__RW"),
            user = "bobby.beans",
          ),
        )
        .body(
          BodyInserters.fromValue(
            mapOf(
              "courtId" to "ACCRYD",
              "courtName" to "A New Court",
              "courtDescription" to "a description",
              "courtType" to "YOUTH",
              "active" to true,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody().json("inserted_court".loadJson())

      assertThat(auditEventMessageCount()).isEqualTo(1)
      val auditMessage = auditMessage()
      assertThatJson(auditMessage).node("what").isEqualTo("COURT_REGISTER_INSERT")
      assertThatJson(auditMessage).node("who").isEqualTo("bobby.beans")
      assertThatJson(auditMessage).node("service").isEqualTo("hmpps-court-register-api")
      assertThatJson(auditMessage).node("details").isNotNull
      assertThatJson(auditMessage).node("when").asString().satisfies(
        Consumer {
          val whenDateTime = LocalDateTime.ofInstant(Instant.parse(it), ZoneOffset.UTC)
          assertThat(whenDateTime).isCloseToUtcNow(within(5, ChronoUnit.SECONDS))
        },
      )
    }

    @Test
    fun `insert a court, buildings and contacts`() {
      val youthCourt = CourtType("YOUTH", "Youth Court")
      val finalCourt = Court("XXXXAA", "Full Court Insert", "A Full court", youthCourt, true)
      val finalBuilding = Building(
        id = 1,
        court = finalCourt,
        subCode = null,
        addressLine2 = "West Cross",
        addressLine1 = "Annex",
        addressLine4 = "Mumble",
        addressLine3 = "Sheffield",
        postcode = "SA4 5TH",
        addressLine5 = "Yorkshire",

        active = true,
      )
      finalCourt.buildings = finalCourt.buildings.plus(finalBuilding)
      val contact = Contact(1, finalBuilding, "TEL", "5555 666666")
      finalBuilding.contacts = finalBuilding.contacts.plus(contact)

      whenever(courtRepository.findById("XXXXAA")).thenReturn(
        Optional.empty(),
        Optional.of(finalCourt),
      )
      whenever(courtTypeRepository.findById("YOUTH")).thenReturn(Optional.of(youthCourt))
      whenever(courtRepository.save(any())).thenReturn(
        finalCourt,
      )

      webTestClient.post()
        .uri("/court-maintenance")
        .accept(MediaType.APPLICATION_JSON)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_COURT_REGISTER__COURT_DETAILS__RW"),
            user = "bobby.beans",
          ),
        )
        .body(
          BodyInserters.fromValue(
            InsertCourtDto(
              "XXXXAA",
              "Full Court Insert",
              "A Full court",
              "YOUTH",
              true,
              listOf(
                UpdateBuildingDto(
                  subCode = null,
                  addressLine2 = "West Cross",
                  addressLine1 = "Annex",
                  addressLine4 = "Mumble",
                  addressLine3 = "Sheffield",
                  postcode = "SA4 5TH",
                  addressLine5 = "Yorkshire",

                  active = true,
                  contacts = listOf(UpdateContactDto("TEL", "5555 666666")),
                ),
              ),
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody().json("inserted_full_court".loadJson())
    }

    @Test
    fun `insert a court with bad data`() {
      webTestClient.post()
        .uri("/court-maintenance")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_COURT_REGISTER__COURT_DETAILS__RW")))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "courtId" to "R".repeat(7),
              "courtName" to "A New Court",
              "courtDescription" to "a description",
              "courtType" to "YOUTH",
              "active" to true,
            ),
          ),
        )
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("errors").value(
          CoreMatchers.hasItems(
            "Court ID must be between 2 and 6",
          ),
        )
    }
  }

  @Nested
  inner class findById {
    @Test
    fun `find court`() {
      val court = Court("ACCRYC", "Accrington Youth Court", null, CourtType("YOUTH", "Youth Court"), true)
      val building1 = Building(
        id = 1,
        court = court,
        subCode = "SUBT11",
        addressLine2 = "West Cross",
        addressLine1 = "Annex",
        addressLine4 = "Mumble",
        addressLine3 = "Sheffield",
        postcode = "SA4 5TH",
        addressLine5 = "Yorkshire",

        active = true,
      )
      val building2 = Building(
        id = 2,
        court = court,
        subCode = null,
        addressLine2 = "West Cross",
        addressLine1 = "Main building",
        addressLine4 = "A Place",
        addressLine3 = "Sheffield",
        postcode = "SA4 5TT",
        addressLine5 = "Yorkshire",

        active = true,
      )

      court.buildings = court.buildings.plus(building1)
      court.buildings = court.buildings.plus(building2)

      val contact1 = Contact(-1, building1, "TEL", "555 666666")
      val contact2 = Contact(-2, building1, "EMAIL", "test@test.com")
      val contact3 = Contact(-3, building2, "TEL", "555 6666655")

      building1.contacts = building1.contacts.plus(contact1)
      building1.contacts = building1.contacts.plus(contact2)
      building2.contacts = building2.contacts.plus(contact3)

      whenever(courtRepository.findById(anyString())).thenReturn(
        Optional.of(court),
      )
      webTestClient.get().uri("/courts/id/ACCRYC")
        .exchange()
        .expectStatus().isOk
        .expectBody().json("court_id_ACCRYC".loadJson())
    }

    @Test
    fun `find court validation failure`() {
      webTestClient.get().uri("/courts/id/1234567890123")
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().json("court_id_badrequest_getCourtFromId".loadJson())
    }
  }

  @Nested
  inner class updateAndInsertBuildings {

    @BeforeEach
    internal fun drainAuditQueue() {
      auditQueue.sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(auditQueue.queueUrl).build())
      await untilCallTo { auditQueue.sqsClient.countMessagesOnQueue(auditQueue.queueUrl).get() } matches { it == 0 }
    }

    @Test
    fun `correct permission are needed to update building data`() {
      webTestClient.put()
        .uri("/court-maintenance/id/ACCRYC/buildings/1")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_DUMMY")))
        .body(
          BodyInserters.fromValue(
            UpdateBuildingDto(
              subCode = "SUBT11",
              addressLine2 = "West Cross",
              addressLine1 = "Annex",
              addressLine4 = "Mumble",
              addressLine3 = "Sheffield",
              postcode = "SA4 5TH",
              addressLine5 = "Yorkshire",

              active = true,
            ),
          ),
        )
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `update a building`() {
      whenever(buildingRepository.findById(1)).thenReturn(
        Optional.of(
          Building(
            id = 1,
            court = Court("ACCRYC", "Accrington Youth Court", null, CourtType("YOUTH", "Youth Court"), true),
            subCode = "SUBBUILD1",
            addressLine2 = "West Cross",
            addressLine1 = "Annex",
            addressLine4 = "Yorkshire",
            addressLine3 = "Sheffield",
            postcode = "S11 9BQ",
            addressLine5 = "South Yorkshire",

            active = true,
          ),
        ),
      )

      whenever(buildingRepository.findBySubCode("SUBT11")).thenReturn(Optional.empty())

      webTestClient.put()
        .uri("/court-maintenance/id/ACCRYC/buildings/1")
        .accept(MediaType.APPLICATION_JSON)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_COURT_REGISTER__COURT_DETAILS__RW"),
            user = "bobby.beans",
          ),
        )
        .body(
          BodyInserters.fromValue(
            UpdateBuildingDto(
              subCode = "SUBT11",
              addressLine2 = "West Cross",
              addressLine1 = "Annex",
              addressLine4 = "Mumble",
              addressLine3 = "Sheffield",
              postcode = "SA4 5TH",
              addressLine5 = "Yorkshire",

              active = true,
            ),
          ),
        )
        .exchange()
        .expectStatus().isOk
        .expectBody().json("updated_building".loadJson())

      assertThat(auditEventMessageCount()).isEqualTo(1)
      val auditMessage = auditMessage()
      assertThatJson(auditMessage).node("what").isEqualTo("COURT_REGISTER_BUILDING_UPDATE")
      assertThatJson(auditMessage).node("who").isEqualTo("bobby.beans")
      assertThatJson(auditMessage).node("service").isEqualTo("hmpps-court-register-api")
      assertThatJson(auditMessage).node("details").isNotNull
      assertThatJson(auditMessage).node("when").asString().satisfies(
        Consumer {
          val whenDateTime = LocalDateTime.ofInstant(Instant.parse(it), ZoneOffset.UTC)
          assertThat(whenDateTime).isCloseToUtcNow(within(5, ChronoUnit.SECONDS))
        },
      )
    }

    @Test
    fun `update a building wtihout sending active flag`() {
      whenever(buildingRepository.findById(2)).thenReturn(
        Optional.of(
          Building(
            id = 2,
            court = Court("ACCRYC", "Accrington Youth Court", null, CourtType("YOUTH", "Youth Court"), true),
            subCode = "SUBBUILD2",
            addressLine2 = "West Cross",
            addressLine1 = "Annex",
            addressLine4 = "Yorkshire",
            addressLine3 = "Sheffield",
            postcode = "S11 9BQ",
            addressLine5 = "South Yorkshire",

            active = true,
          ),
        ),
      )

      whenever(buildingRepository.findBySubCode("SUBT22")).thenReturn(Optional.empty())

      webTestClient.put()
        .uri("/court-maintenance/id/ACCRYC/buildings/2")
        .accept(MediaType.APPLICATION_JSON)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_COURT_REGISTER__COURT_DETAILS__RW"),
            user = "bobby.beans",
          ),
        )
        .body(
          BodyInserters.fromValue(
            UpdateBuildingDto(
              subCode = "SUBT22",
              addressLine2 = "West Cross",
              addressLine1 = "Annex",
              addressLine4 = "Mumble",
              addressLine3 = "Sheffield",
              postcode = "SA4 5TH",
              addressLine5 = "Yorkshire",

              // missing active flag
            ),
          ),
        )
        .exchange()
        .expectStatus().isOk
        .expectBody().json("updated_building_2".loadJson())

      assertThat(auditEventMessageCount()).isEqualTo(1)
      val auditMessage = auditMessage()
      assertThatJson(auditMessage).node("what").isEqualTo("COURT_REGISTER_BUILDING_UPDATE")
      assertThatJson(auditMessage).node("who").isEqualTo("bobby.beans")
      assertThatJson(auditMessage).node("service").isEqualTo("hmpps-court-register-api")
      assertThatJson(auditMessage).node("details").isNotNull
      assertThatJson(auditMessage).node("when").asString().satisfies(
        Consumer {
          val whenDateTime = LocalDateTime.ofInstant(Instant.parse(it), ZoneOffset.UTC)
          assertThat(whenDateTime).isCloseToUtcNow(within(5, ChronoUnit.SECONDS))
        },
      )
    }

    @Test
    fun `will not update a building when data is too long`() {
      whenever(buildingRepository.findById(1)).thenReturn(
        Optional.of(
          Building(
            id = 1,
            court = Court("ACCRYC", "Accrington Youth Court", null, CourtType("YOUTH", "Youth Court"), true),
            subCode = "SUBBUILD1",
            addressLine2 = "West Cross",
            addressLine1 = "Annex",
            addressLine4 = "Yorkshire",
            addressLine3 = "Sheffield",
            postcode = "S11 9BQ",
            addressLine5 = "South Yorkshire",

            active = true,
          ),
        ),
      )

      whenever(buildingRepository.findBySubCode("SUBT11")).thenReturn(Optional.empty())

      webTestClient.put()
        .uri("/court-maintenance/id/ACCRYC/buildings/1")
        .accept(MediaType.APPLICATION_JSON)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_COURT_REGISTER__COURT_DETAILS__RW"),
            user = "bobby.beans",
          ),
        )
        .body(
          BodyInserters.fromValue(
            UpdateBuildingDto(
              subCode = "A".repeat(7),
              addressLine2 = "A".repeat(81),
              addressLine1 = "A".repeat(51),
              addressLine4 = "A".repeat(81),
              addressLine3 = "A".repeat(81),
              postcode = "A".repeat(9),
              addressLine5 = "A".repeat(81),
              active = true,
            ),
          ),
        )
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("errors").value(
          CoreMatchers.hasItems(
            "Postcode must be no more than 8 characters",
            "Address Line 5 must be no more than 80 characters",
            "Address Line 1 must be no more than 50 characters",
            "Address Line 2 must be no more than 80 characters",
            "Address Line 3 must be no more than 80 characters",
            "Address Line 4 must be no more than 80 characters",
            "Sub location code must be no more than 6 characters",
          ),
        )
    }

    @Test
    fun `insert a building`() {
      val court = Court("ACCRYC", "Accrington Youth Court", null, CourtType("YOUTH", "Youth Court"), true)
      whenever(courtRepository.findById("ACCRYD")).thenReturn(
        Optional.of(court),
      )

      val createdBuilding = Building(
        court = court,
        subCode = "SUBT11",
        addressLine2 = "West Cross",
        addressLine1 = "Annex",
        addressLine4 = "Mumble",
        addressLine3 = "Sheffield",
        postcode = "SA4 5TH",
        addressLine5 = "Yorkshire",

        active = true,
      )

      val updatedBuilding = createdBuilding.copy(id = 1)

      whenever(buildingRepository.save(createdBuilding)).thenReturn(
        updatedBuilding,
      )

      webTestClient.post()
        .uri("/court-maintenance/id/ACCRYD/buildings")
        .accept(MediaType.APPLICATION_JSON)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_COURT_REGISTER__COURT_DETAILS__RW"),
            user = "bobby.beans",
          ),
        )
        .body(
          BodyInserters.fromValue(
            UpdateBuildingDto(
              subCode = "SUBT11",
              addressLine2 = "West Cross",
              addressLine1 = "Annex",
              addressLine4 = "Mumble",
              addressLine3 = "Sheffield",
              postcode = "SA4 5TH",
              addressLine5 = "Yorkshire",

              active = true,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody().json("inserted_building".loadJson())

      assertThat(auditEventMessageCount()).isEqualTo(1)
      val auditMessage = auditMessage()
      assertThatJson(auditMessage).node("what").isEqualTo("COURT_REGISTER_BUILDING_INSERT")
      assertThatJson(auditMessage).node("who").isEqualTo("bobby.beans")
      assertThatJson(auditMessage).node("service").isEqualTo("hmpps-court-register-api")
      assertThatJson(auditMessage).node("details").isNotNull
      assertThatJson(auditMessage).node("when").asString().satisfies(
        Consumer {
          val whenDateTime = LocalDateTime.ofInstant(Instant.parse(it), ZoneOffset.UTC)
          assertThat(whenDateTime).isCloseToUtcNow(within(5, ChronoUnit.SECONDS))
        },
      )
    }

    @Test
    fun `insert a building without sending active flag`() {
      val court = Court("ACCRYC", "Accrington Youth Court", null, CourtType("YOUTH", "Youth Court"), true)
      whenever(courtRepository.findById("ACCRYD")).thenReturn(
        Optional.of(court),
      )

      val createdBuilding = Building(
        court = court,
        subCode = "SUBT22",
        addressLine2 = "West Cross",
        addressLine1 = "Annex",
        addressLine4 = "Mumble",
        addressLine3 = "Sheffield",
        postcode = "SA4 5TH",
        addressLine5 = "Yorkshire",

        active = true,
      )

      val updatedBuilding = createdBuilding.copy(id = 2)

      whenever(buildingRepository.save(createdBuilding)).thenReturn(
        updatedBuilding,
      )

      webTestClient.post()
        .uri("/court-maintenance/id/ACCRYD/buildings")
        .accept(MediaType.APPLICATION_JSON)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_COURT_REGISTER__COURT_DETAILS__RW"),
            user = "bobby.beans",
          ),
        )
        .body(
          BodyInserters.fromValue(
            UpdateBuildingDto(
              subCode = "SUBT22",
              addressLine2 = "West Cross",
              addressLine1 = "Annex",
              addressLine4 = "Mumble",
              addressLine3 = "Sheffield",
              postcode = "SA4 5TH",
              addressLine5 = "Yorkshire",

              // missing active flag
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody().json("inserted_building_2".loadJson())

      assertThat(auditEventMessageCount()).isEqualTo(1)
      val auditMessage = auditMessage()
      assertThatJson(auditMessage).node("what").isEqualTo("COURT_REGISTER_BUILDING_INSERT")
      assertThatJson(auditMessage).node("who").isEqualTo("bobby.beans")
      assertThatJson(auditMessage).node("service").isEqualTo("hmpps-court-register-api")
      assertThatJson(auditMessage).node("details").isNotNull
      assertThatJson(auditMessage).node("when").asString().satisfies(
        Consumer {
          val whenDateTime = LocalDateTime.ofInstant(Instant.parse(it), ZoneOffset.UTC)
          assertThat(whenDateTime).isCloseToUtcNow(within(5, ChronoUnit.SECONDS))
        },
      )
    }

    @Test
    fun `will not insert a building where the data is too long`() {
      val court = Court("ACCRYC", "Accrington Youth Court", null, CourtType("YOUTH", "Youth Court"), true)
      whenever(courtRepository.findById("ACCRYD")).thenReturn(
        Optional.of(court),
      )

      val createdBuilding = Building(
        court = court,
        subCode = "SUBT11",
        addressLine2 = "West Cross",
        addressLine1 = "Annex",
        addressLine4 = "Mumble",
        addressLine3 = "Sheffield",
        postcode = "SA4 5TH",
        addressLine5 = "Yorkshire",

        active = true,
      )

      val updatedBuilding = createdBuilding.copy(id = 1)

      whenever(buildingRepository.save(createdBuilding)).thenReturn(
        updatedBuilding,
      )

      webTestClient.post()
        .uri("/court-maintenance/id/ACCRYD/buildings")
        .accept(MediaType.APPLICATION_JSON)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_COURT_REGISTER__COURT_DETAILS__RW"),
            user = "bobby.beans",
          ),
        )
        .body(
          BodyInserters.fromValue(
            UpdateBuildingDto(
              subCode = "A".repeat(7),
              addressLine2 = "A".repeat(81),
              addressLine1 = "A".repeat(51),
              addressLine4 = "A".repeat(81),
              addressLine3 = "A".repeat(81),
              postcode = "A".repeat(9),
              addressLine5 = "A".repeat(81),
              active = true,
            ),
          ),
        )
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
        .jsonPath("errors").value(
          CoreMatchers.hasItems(
            "Postcode must be no more than 8 characters",
            "Address Line 5 must be no more than 80 characters",
            "Address Line 1 must be no more than 50 characters",
            "Address Line 2 must be no more than 80 characters",
            "Address Line 3 must be no more than 80 characters",
            "Address Line 4 must be no more than 80 characters",
            "Sub location code must be no more than 6 characters",
          ),
        )
    }
  }

  @Nested
  inner class updateAndInsertContacts {

    @BeforeEach
    internal fun drainAuditQueue() {
      auditQueue.sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(auditQueue.queueUrl).build())
      await untilCallTo { auditQueue.sqsClient.countMessagesOnQueue(auditQueue.queueUrl).get() } matches { it == 0 }
    }

    @Test
    fun `correct permission are needed to update contact data`() {
      webTestClient.put()
        .uri("/court-maintenance/id/ACCRYC/buildings/1/contacts/1")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_DUMMY")))
        .body(BodyInserters.fromValue(UpdateContactDto("TEL", "5555 666666")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `update a contact`() {
      val court = Court("ACCRYC", "Accrington Youth Court", null, CourtType("YOUTH", "Youth Court"), true)
      val building = Building(
        id = 1,
        court = court,
        subCode = "SUBT11",
        addressLine2 = "West Cross",
        addressLine1 = "Annex",
        addressLine4 = "Mumble",
        addressLine3 = "Sheffield",
        postcode = "SA4 5TH",
        addressLine5 = "Yorkshire",

        active = true,
      )

      whenever(contactRepository.findById(1)).thenReturn(
        Optional.of(Contact(building = building, type = "TEL", detail = "5555 6666666", id = 1)),
      )

      webTestClient.put()
        .uri("/court-maintenance/id/ACCRYC/buildings/1/contacts/1")
        .accept(MediaType.APPLICATION_JSON)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_COURT_REGISTER__COURT_DETAILS__RW"),
            user = "bobby.beans",
          ),
        )
        .body(BodyInserters.fromValue(UpdateContactDto("TEL", "7777 22222222")))
        .exchange()
        .expectStatus().isOk
        .expectBody().json("updated_contact".loadJson())

      assertThat(auditEventMessageCount()).isEqualTo(1)
      val auditMessage = auditMessage()
      assertThatJson(auditMessage).node("what").isEqualTo("COURT_REGISTER_CONTACT_UPDATE")
      assertThatJson(auditMessage).node("who").isEqualTo("bobby.beans")
      assertThatJson(auditMessage).node("service").isEqualTo("hmpps-court-register-api")
      assertThatJson(auditMessage).node("details").isNotNull
      assertThatJson(auditMessage).node("when").asString().satisfies(
        Consumer {
          val whenDateTime = LocalDateTime.ofInstant(Instant.parse(it), ZoneOffset.UTC)
          assertThat(whenDateTime).isCloseToUtcNow(within(5, ChronoUnit.SECONDS))
        },
      )
    }

    @Test
    fun `will not update a contact when the details are too long`() {
      val court = Court("ACCRYC", "Accrington Youth Court", null, CourtType("YOUTH", "Youth Court"), true)
      val building = Building(
        id = 1,
        court = court,
        subCode = "SUBT11",
        addressLine2 = "West Cross",
        addressLine1 = "Annex",
        addressLine4 = "Mumble",
        addressLine3 = "Sheffield",
        postcode = "SA4 5TH",
        addressLine5 = "Yorkshire",

        active = true,
      )

      whenever(contactRepository.findById(1)).thenReturn(
        Optional.of(Contact(building = building, type = "TEL", detail = "5555 6666666", id = 1)),
      )

      webTestClient.put()
        .uri("/court-maintenance/id/ACCRYC/buildings/1/contacts/1")
        .accept(MediaType.APPLICATION_JSON)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_COURT_REGISTER__COURT_DETAILS__RW"),
            user = "bobby.beans",
          ),
        )
        .body(BodyInserters.fromValue(UpdateContactDto("TEL", "1".repeat(81))))
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun `insert a contact`() {
      val court = Court("ACCRYC", "Accrington Youth Court", null, CourtType("YOUTH", "Youth Court"), true)
      val building = Building(
        id = 1,
        court = court,
        subCode = "SUBT11",
        addressLine2 = "West Cross",
        addressLine1 = "Annex",
        addressLine4 = "Mumble",
        addressLine3 = "Sheffield",
        postcode = "SA4 5TH",
        addressLine5 = "Yorkshire",

        active = true,
      )
      building.contacts = building.contacts.plus(Contact(id = 1, type = "TEL", detail = "5555 33333", building = building))

      whenever(buildingRepository.findById(1)).thenReturn(
        Optional.of(building),
      )

      val contactToSave = Contact(type = "EMAIL", detail = "test@test.com", building = building)

      whenever(contactRepository.save(contactToSave)).thenReturn(
        contactToSave.copy(id = 2),
      )

      webTestClient.post()
        .uri("/court-maintenance/id/ACCRYC/buildings/1/contacts")
        .accept(MediaType.APPLICATION_JSON)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_COURT_REGISTER__COURT_DETAILS__RW"),
            user = "bobby.beans",
          ),
        )
        .body(BodyInserters.fromValue(UpdateContactDto("EMAIL", "test@test.com")))
        .exchange()
        .expectStatus().isCreated
        .expectBody().json("inserted_contact".loadJson())

      assertThat(auditEventMessageCount()).isEqualTo(1)
      val auditMessage = auditMessage()
      assertThatJson(auditMessage).node("what").isEqualTo("COURT_REGISTER_CONTACT_INSERT")
      assertThatJson(auditMessage).node("who").isEqualTo("bobby.beans")
      assertThatJson(auditMessage).node("service").isEqualTo("hmpps-court-register-api")
      assertThatJson(auditMessage).node("details").isNotNull
      assertThatJson(auditMessage).node("when").asString().satisfies(
        Consumer {
          val whenDateTime = LocalDateTime.ofInstant(Instant.parse(it), ZoneOffset.UTC)
          assertThat(whenDateTime).isCloseToUtcNow(within(5, ChronoUnit.SECONDS))
        },
      )
    }

    @Test
    fun `will not insert a contact when the detail is too long`() {
      val court = Court("ACCRYC", "Accrington Youth Court", null, CourtType("YOUTH", "Youth Court"), true)
      val building = Building(
        id = 1,
        court = court,
        subCode = "SUBT11",
        addressLine2 = "West Cross",
        addressLine1 = "Annex",
        addressLine4 = "Mumble",
        addressLine3 = "Sheffield",
        postcode = "SA4 5TH",
        addressLine5 = "Yorkshire",

        active = true,
      )
      building.contacts = building.contacts.plus(Contact(id = 1, type = "TEL", detail = "5555 33333", building = building))

      whenever(buildingRepository.findById(1)).thenReturn(
        Optional.of(building),
      )

      val contactToSave = Contact(type = "EMAIL", detail = "test@test.com", building = building)

      whenever(contactRepository.save(contactToSave)).thenReturn(
        contactToSave.copy(id = 2),
      )

      webTestClient.post()
        .uri("/court-maintenance/id/ACCRYC/buildings/1/contacts")
        .accept(MediaType.APPLICATION_JSON)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_COURT_REGISTER__COURT_DETAILS__RW"),
            user = "bobby.beans",
          ),
        )
        .body(BodyInserters.fromValue(UpdateContactDto("TEL", "1".repeat(81))))
        .exchange()
        .expectStatus().isBadRequest
    }
  }

  private fun String.loadJson(): String = CourtResourceTest::class.java.getResource("$this.json")?.readText()
    ?: throw AssertionError("file $this.json not found")

  fun auditEventMessageCount(): Int? = auditSqsClient.countMessagesOnQueue(auditQueue.queueUrl).get()

  fun auditMessage(): String? = auditSqsClient.receiveMessage(ReceiveMessageRequest.builder().queueUrl(auditQueueUrl).build()).get().messages()[0].body()
}

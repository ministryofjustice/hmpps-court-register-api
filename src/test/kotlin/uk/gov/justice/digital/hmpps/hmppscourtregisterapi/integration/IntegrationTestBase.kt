package uk.gov.justice.digital.hmpps.hmppscourtregisterapi.integration

import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.testcontainers.LocalStackContainer
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.testcontainers.PostgresContainer
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.wiremock.HmppsAuthApiExtension
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.wiremock.HmppsAuthApiExtension.Companion.hmppsAuth
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.wiremock.PrisonApiExtension
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.wiremock.SDRSApiExtension
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper

@ExtendWith(SDRSApiExtension::class, PrisonApiExtension::class, HmppsAuthApiExtension::class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebTestClient
abstract class IntegrationTestBase {

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthorisationHelper

  @Autowired
  protected lateinit var hmppsQueueService: HmppsQueueService

  internal val auditQueue by lazy {
    hmppsQueueService.findByQueueId("audit")
      ?: throw RuntimeException("Queue with name audit doesn't exist")
  }

  internal val auditSqsClient by lazy { auditQueue.sqsClient }
  internal val auditQueueUrl: String by lazy { auditQueue.queueUrl }

  init {
    // Resolves an issue where Wiremock keeps previous sockets open from other tests causing connection resets
    System.setProperty("http.keepAlive", "false")
  }

  @BeforeEach
  fun cleanQueues() {
    auditQueue.sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(auditQueue.queueUrl).build())
    await untilCallTo { auditQueue.sqsClient.countMessagesOnQueue(auditQueue.queueUrl).get() } matches { it == 0 }
  }

  internal fun HttpHeaders.authToken(roles: List<String> = emptyList(), user: String = "court-reg-client") {
    this.setBearerAuth(
      jwtAuthHelper.createJwtAccessToken(
        roles = roles,
        clientId = "some-client",
        username = user,
      ),
    )
  }

  protected fun stubPingWithResponse(status: Int) {
    hmppsAuth.stubHealthPing(status)
  }

  companion object {
    private val pgContainer = PostgresContainer.instance
    private val localStackContainer = LocalStackContainer.instance

    @JvmStatic
    @DynamicPropertySource
    fun properties(registry: DynamicPropertyRegistry) {
      pgContainer?.run {
        registry.add("spring.datasource.url", pgContainer::getJdbcUrl)
        registry.add("spring.datasource.username", pgContainer::getUsername)
        registry.add("spring.datasource.password", pgContainer::getPassword)
      }

      System.setProperty("aws.region", "eu-west-2")

      localStackContainer?.also { LocalStackContainer.setLocalStackProperties(it, registry) }
    }
  }
}

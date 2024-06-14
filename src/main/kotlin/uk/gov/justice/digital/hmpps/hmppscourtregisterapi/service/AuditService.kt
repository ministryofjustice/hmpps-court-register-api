package uk.gov.justice.digital.hmpps.hmppscourtregisterapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.config.SecurityUserContext
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import java.time.Instant

@Service
class AuditService(
  hmppsQueueService: HmppsQueueService,
  @Value("\${spring.application.name}")
  private val serviceName: String,
  private val securityUserContext: SecurityUserContext,
  private val objectMapper: ObjectMapper,
) {
  private val auditQueue by lazy {
    hmppsQueueService.findByQueueId("audit") ?: throw RuntimeException("Queue with name audit doesn't exist")
  }
  private val auditSqsClient by lazy { auditQueue.sqsClient }
  private val auditQueueUrl by lazy { auditQueue.queueUrl }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun sendAuditEvent(what: String, details: Any) {
    val auditEvent = AuditEvent(
      what = what,
      who = securityUserContext.principal,
      service = serviceName,
      details = objectMapper.writeValueAsString(details),
    )
    log.debug("Audit {} ", auditEvent)

    auditSqsClient.sendMessage(
      SendMessageRequest.builder()
        .queueUrl(auditQueueUrl)
        .messageBody(auditEvent.toJson())
        .build(),
    )
  }
  private fun Any.toJson() = objectMapper.writeValueAsString(this)
}

data class AuditEvent(
  val what: String,
  val `when`: Instant = Instant.now(),
  val who: String,
  val service: String,
  val details: String? = null,
)

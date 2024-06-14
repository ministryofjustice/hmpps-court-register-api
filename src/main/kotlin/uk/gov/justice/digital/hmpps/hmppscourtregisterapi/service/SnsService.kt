package uk.gov.justice.digital.hmpps.hmppscourtregisterapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.hmpps.sqs.HmppsQueueService

@Service
class SnsService(
  hmppsQueueService: HmppsQueueService,
  private val objectMapper: ObjectMapper,
) {
  private val domaineventsTopic by lazy {
    hmppsQueueService.findByTopicId("domainevents")
      ?: throw RuntimeException("Topic with name domainevents doesn't exist")
  }
  private val domaineventsTopicClient by lazy { domaineventsTopic.snsClient }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun sendEvent(eventType: EventType, id: String) {
    log.debug("Event {} for id {}", eventType)
    domaineventsTopicClient.publish(
      PublishRequest.builder()
        .topicArn(domaineventsTopic.arn)
        .message(objectMapper.writeValueAsString(RegisterChangeEvent(eventType, id)))
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String").stringValue(eventType.name).build(),
          ),
        )
        .build()
        .also { log.info("Published event ${eventType.name} to domainevents topic") },
    )
  }
}

data class RegisterChangeEvent(
  val eventType: EventType,
  val id: String,
)

enum class AuditType {
  COURT_REGISTER_INSERT,
  COURT_REGISTER_UPDATE,
  COURT_REGISTER_BUILDING_INSERT,
  COURT_REGISTER_BUILDING_UPDATE,
  COURT_REGISTER_BUILDING_DELETE,
  COURT_REGISTER_CONTACT_INSERT,
  COURT_REGISTER_CONTACT_UPDATE,
  COURT_REGISTER_CONTACT_DELETE,
}

enum class EventType {
  COURT_REGISTER_UPDATE,
}

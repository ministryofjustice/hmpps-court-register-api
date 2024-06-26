package uk.gov.justice.digital.hmpps.hmppscourtregisterapi.client.dto

import com.fasterxml.jackson.databind.PropertyNamingStrategies.UpperCamelCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import java.time.ZonedDateTime

@JsonNaming(UpperCamelCaseStrategy::class)
data class MessageHeader(
  val from: String = "CONSUMER_APPLICATION",
  val messageID: MessageID = MessageID(),
  val messageType: String = "GetReference",
  val timeStamp: ZonedDateTime = ZonedDateTime.now(),
  val to: String = "SDRS_AZURE",
)

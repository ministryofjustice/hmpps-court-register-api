package uk.gov.justice.digital.hmpps.hmppscourtregisterapi.client.sdrs.dto

import com.fasterxml.jackson.databind.PropertyNamingStrategies.UpperCamelCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(UpperCamelCaseStrategy::class)
data class SDRSRequest(
  val messageBody: MessageBodyRequest = MessageBodyRequest(),
  val messageHeader: MessageHeader = MessageHeader(),
)

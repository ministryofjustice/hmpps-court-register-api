package uk.gov.justice.digital.hmpps.hmppscourtregisterapi.client.sdrs.dto

import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class SDRSResponse(
  val messageBody: MessageBodyResponse,
  val messageStatus: MessageStatusResponse,
)

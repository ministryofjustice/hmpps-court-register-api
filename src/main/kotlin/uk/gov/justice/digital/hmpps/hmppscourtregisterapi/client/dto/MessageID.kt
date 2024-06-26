package uk.gov.justice.digital.hmpps.hmppscourtregisterapi.client.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategies.UpperCamelCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import java.util.*

@JsonNaming(UpperCamelCaseStrategy::class)
data class MessageID(
  @JsonProperty("UUID")
  val uuid: UUID = UUID.randomUUID(),
)

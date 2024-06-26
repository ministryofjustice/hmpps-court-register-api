package uk.gov.justice.digital.hmpps.hmppscourtregisterapi.client.dto

import com.fasterxml.jackson.databind.PropertyNamingStrategies.UpperCamelCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(UpperCamelCaseStrategy::class)
data class ConsumerReferenceRequest(
  val referenceType: String = "getOrganisationUnits"
)

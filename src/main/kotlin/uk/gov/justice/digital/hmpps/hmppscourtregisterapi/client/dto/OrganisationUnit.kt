package uk.gov.justice.digital.hmpps.hmppscourtregisterapi.client.dto

import com.fasterxml.jackson.databind.PropertyNamingStrategies.UpperCamelCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(UpperCamelCaseStrategy::class)
data class OrganisationUnit(
  val oUCodeL3Name: String,
  val oUCodeL1Name: String,
  val phoneNumber: String?,
  val addressLine1: String,
  val addressLine2: String,
  val addressLine3: String,
  val postCode: String,
  val oUCode: String,
)

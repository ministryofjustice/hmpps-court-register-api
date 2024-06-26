package uk.gov.justice.digital.hmpps.hmppscourtregisterapi.client.dto

data class MessageStatusResponse(
  val status: String,
  val code: String? = null,
  val reason: String? = null,
)

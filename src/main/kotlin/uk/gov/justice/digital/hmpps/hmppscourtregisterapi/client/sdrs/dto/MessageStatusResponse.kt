package uk.gov.justice.digital.hmpps.hmppscourtregisterapi.client.sdrs.dto

data class MessageStatusResponse(
  val status: String,
  val code: String? = null,
  val reason: String? = null,
)

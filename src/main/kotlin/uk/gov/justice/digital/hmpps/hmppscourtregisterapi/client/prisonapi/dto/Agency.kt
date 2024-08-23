package uk.gov.justice.digital.hmpps.hmppscourtregisterapi.client.prisonapi.dto

data class Agency(
  val agencyId: String,
  val description: String,
  val longDescription: String?,
  val active: Boolean,
  val courtType: String?,
  val courtTypeDescription: String?,
)

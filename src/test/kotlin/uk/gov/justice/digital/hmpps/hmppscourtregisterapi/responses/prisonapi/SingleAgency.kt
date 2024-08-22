package uk.gov.justice.digital.hmpps.hmppscourtregisterapi.responses.prisonapi

fun singleAgency() = """
  [
    {
      "agencyId": "COURT1",
      "description": "Court Mc",
      "longDescription": "Court Magistrates' Court",
      "agencyType": "CRT",
      "active": true,
      "courtType": "MC",
      "courtTypeDescription": "Magistrates Court"
    }
  ]
""".trimIndent()

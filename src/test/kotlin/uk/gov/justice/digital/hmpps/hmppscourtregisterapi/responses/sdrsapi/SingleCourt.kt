package uk.gov.justice.digital.hmpps.hmppscourtregisterapi.responses.sdrsapi

import java.util.*

fun singleCourt() = """
  {
    "MessageHeader" : {
      "MessageID" : {
        "UUID" : "${UUID.randomUUID()}",
        "RelatesTo" : "{${UUID.randomUUID()}}"
      },
      "TimeStamp" : "2024-06-18",
      "MessageType" : "getReference",
      "From" : "SDRS_AZURE",
      "To" : "CONSUMER_APPLICATION"
    },
    "MessageBody" : {
      "GatewayOperationType" : {
        "OrganisationUnitResponse" : {
          "OrganisationUnit" : [ {
            "Key" : "9999",
            "OUCodeL3Code" : "XX",
            "OUCodeL3Name" : "The Magistrates' Court",
            "OUCodeL1Code" : "B",
            "OUCodeL1Name" : "Magistrates",
            "OUCodeL2Code" : "05",
            "OUCodeL2Name" : "South Yorkshire",
            "PhoneNumber" : "01111 111222",
            "AddressLine1" : "Court Street",
            "AddressLine2" : "Sheffield",
            "AddressLine3" : "South Yorkshire",
            "PostCode" : "S1 1AA",
            "IsWelsh" : false,
            "CPP_UUID" : "dummy UUID",
            "OUCode" : "B05XX00",
            "CourtID" : "1",
            "StartDate" : "2006-10-01",
            "ChangedDateTime" : "2023-12-06T00:00:02"
          }]
        }
      }
    },
    "MessageStatus" : {
      "status" : "SUCCESS",
      "code" : " ",
      "reason" : " ",
      "detail" : " "
    }
  }
""".trimIndent()

package uk.gov.justice.digital.hmpps.hmppscourtregisterapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class HmppsCourtRegisterApi

fun main(args: Array<String>) {
  runApplication<HmppsCourtRegisterApi>(*args)
}

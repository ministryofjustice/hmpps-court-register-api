package uk.gov.justice.digital.hmpps.hmppscourtregisterapi.resource

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.service.SDRSService

@RestController
@RequestMapping("/admin")
class AdminResource(private val sdrsService: SDRSService) {

  @PostMapping("/refresh-data")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun refreshData() {
    sdrsService.refreshData()
  }
}

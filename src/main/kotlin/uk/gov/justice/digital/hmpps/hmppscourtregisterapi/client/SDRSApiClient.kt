package uk.gov.justice.digital.hmpps.hmppscourtregisterapi.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.client.dto.SDRSRequest
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.client.dto.SDRSResponse

@Component
class SDRSApiClient(@Qualifier("standingDataReferenceServiceApiWebClient") private val webClient: WebClient) {
  private inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}

  fun callSDRS(sdrsRequest: SDRSRequest): SDRSResponse {
    return webClient.post()
      .uri("/cld_StandingDataReferenceService/service/sdrs/sdrs/sdrsApi")
      .bodyValue(sdrsRequest)
      .retrieve()
      .bodyToMono(typeReference<SDRSResponse>())
      .block()!!
  }
}

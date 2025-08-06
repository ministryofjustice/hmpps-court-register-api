package uk.gov.justice.digital.hmpps.hmppscourtregisterapi.client.prisonapi

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.client.prisonapi.dto.Agency

@Component
class PrisonApiClient(@Qualifier("prisonApiWebClient") private val webClient: WebClient) {
  private inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}

  fun getCourtAgencies(): List<Agency> = webClient.get()
    .uri {
      it.path("/api/agencies/type/CRT")
        .queryParam("activeOnly", false)
        .build()
    }
    .retrieve()
    .bodyToMono(typeReference<List<Agency>>())
    .block()!!
}

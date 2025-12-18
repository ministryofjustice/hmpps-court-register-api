package uk.gov.justice.digital.hmpps.hmppscourtregisterapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.util.MultiValueMap
import org.springframework.util.unit.DataSize
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.hmpps.kotlin.auth.healthWebClient
import java.time.Duration

@Configuration
class WebClientConfiguration(
  @param:Value("\${sdrs.api.url}") private val standingDataReferenceServiceApiUrl: String,
  @param:Value("\${prison.api.url}") private val prisonApiUri: String,
  @param:Value("\${oauth.endpoint.url}") val hmppsAuthBaseUri: String,
  @param:Value("\${api.health-timeout:2s}") val healthTimeout: Duration,
) {

  // HMPPS Auth health ping is required if your service calls HMPPS Auth to get a token to call other services
  @Bean
  fun hmppsAuthHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(hmppsAuthBaseUri, healthTimeout)

  @Bean
  fun standingDataReferenceServiceApiWebClient(builder: WebClient.Builder): WebClient = builder.baseUrl(standingDataReferenceServiceApiUrl)
    .defaultHeaders { headers -> headers.addAll(createHeaders()) }
    .build()

  @Bean
  fun prisonApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager): WebClient {
    val size = DataSize.ofMegabytes(16).toBytes().toInt()
    val strategies = ExchangeStrategies.builder().codecs { it.defaultCodecs().maxInMemorySize(size) }.build()
    val filter = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
    filter.setDefaultClientRegistrationId("hmpps-api")
    return WebClient.builder()
      .baseUrl(prisonApiUri)
      .exchangeStrategies(strategies)
      .filter(filter)
      .build()
  }

  @Bean
  fun authorizedClientManager(
    clientRegistrationRepository: ClientRegistrationRepository,
    oAuth2AuthorizedClientService: OAuth2AuthorizedClientService,
  ): OAuth2AuthorizedClientManager = AuthorizedClientServiceOAuth2AuthorizedClientManager(
    clientRegistrationRepository,
    oAuth2AuthorizedClientService,
  ).apply {
    setAuthorizedClientProvider(OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build())
  }

  private fun createHeaders(): MultiValueMap<String, String> {
    val headers = HttpHeaders()
    headers.add(HttpHeaders.CONTENT_TYPE, "application/json")
    headers.add(HttpHeaders.ACCEPT, "application/json")
    return headers
  }
}

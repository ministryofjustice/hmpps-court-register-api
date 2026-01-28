package uk.gov.justice.digital.hmpps.hmppscourtregisterapi.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.responses.sdrsapi.singleCourt

class SDRSApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {

  companion object {
    @JvmField
    val sdrsApi = SDRSApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    sdrsApi.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    sdrsApi.resetRequests()
  }
  override fun afterAll(context: ExtensionContext) {
    sdrsApi.stop()
  }
}

class SDRSApiMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 8332
  }

  fun stubGetSingleCourt(): StubMapping = stubFor(
    post("/cld_StandingDataReferenceService/service/sdrs/sdrs/sdrsApi")
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(singleCourt())
          .withStatus(200),
      ),
  )
}

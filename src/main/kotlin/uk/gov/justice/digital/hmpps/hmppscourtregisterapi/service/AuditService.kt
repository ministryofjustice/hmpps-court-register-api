package uk.gov.justice.digital.hmpps.hmppscourtregisterapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.config.SecurityUserContext
import uk.gov.justice.hmpps.sqs.audit.HmppsAuditEvent
import uk.gov.justice.hmpps.sqs.audit.HmppsAuditService

@Service
class AuditService(
  private val hmppsAuditService: HmppsAuditService,
  @Value("\${spring.application.name}")
  private val serviceName: String,
  private val securityUserContext: SecurityUserContext,
  private val objectMapper: ObjectMapper,
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun sendAuditEvent(what: String, details: Any) {
    val auditEvent = HmppsAuditEvent(
      what = what,
      who = securityUserContext.principal,
      service = serviceName,
      details = objectMapper.writeValueAsString(details),
    )
    log.debug("Audit {} ", auditEvent)
    runBlocking {
      hmppsAuditService.publishEvent(auditEvent)
    }
  }
}

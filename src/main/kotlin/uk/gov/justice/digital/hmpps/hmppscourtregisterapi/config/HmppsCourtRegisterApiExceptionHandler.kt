package uk.gov.justice.digital.hmpps.hmppscourtregisterapi.config

import jakarta.persistence.EntityExistsException
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.ResponseEntity
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
class HmppsCourtRegisterApiExceptionHandler {
  @ExceptionHandler(ValidationException::class)
  fun handleValidationException(e: ValidationException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(BAD_REQUEST)
    .body(
      ErrorResponse(
        status = BAD_REQUEST,
        userMessage = "Validation failure: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.info("Validation exception: {}", e.message) }

  @ExceptionHandler(MissingServletRequestParameterException::class)
  fun handleMissingServletRequestParameterException(e: MissingServletRequestParameterException): ResponseEntity<ErrorResponse> = ResponseEntity.status(BAD_REQUEST)
    .body(
      ErrorResponse(
        status = BAD_REQUEST,
        userMessage = "Missing Request Parameter: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.info("Missing Request Parameter exception: {}", e.message) }

  @ExceptionHandler(NoResourceFoundException::class)
  fun handleNoResourceFoundException(e: NoResourceFoundException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(NOT_FOUND)
    .body(
      ErrorResponse(
        status = NOT_FOUND,
        userMessage = "No resource found failure: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.info("No resource found exception: {}", e.message) }

  @ExceptionHandler(EntityNotFoundException::class)
  fun handleNotFoundException(e: EntityNotFoundException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(NOT_FOUND)
    .body(ErrorResponse(status = NOT_FOUND, developerMessage = e.message)).also { log.debug("Court not found exception: {}", e.message) }

  @ExceptionHandler(EntityExistsException::class)
  fun handleExistsException(e: EntityExistsException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(HttpStatus.CONFLICT)
    .body(ErrorResponse(status = HttpStatus.CONFLICT, developerMessage = e.message)).also { log.debug("Court already exists exception: {}", e.message) }

  @ExceptionHandler(DataIntegrityViolationException::class)
  fun handleConstraintViolationException(e: DataIntegrityViolationException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(HttpStatus.CONFLICT)
    .body(ErrorResponse(status = HttpStatus.CONFLICT, developerMessage = e.message)).also { log.debug("Unable to update court due to : {}", e.message) }

  @ExceptionHandler(MethodArgumentNotValidException::class)
  fun handleValidationAnyException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(BAD_REQUEST)
    .body(ErrorResponse(status = BAD_REQUEST, developerMessage = e.message, errors = e.asErrorList())).also { log.info("Method argument not valid Exception: {}", e.message) }

  @ExceptionHandler(AuthorizationDeniedException::class)
  fun handleAuthorizationDeniedException(e: AuthorizationDeniedException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(HttpStatus.FORBIDDEN)
    .body(ErrorResponse(status = HttpStatus.FORBIDDEN))

  @ExceptionHandler(Exception::class)
  fun handleException(e: Exception): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(INTERNAL_SERVER_ERROR)
    .body(
      ErrorResponse(
        status = INTERNAL_SERVER_ERROR,
        userMessage = "Unexpected error: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.error("Unexpected exception", e) }

  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

private fun MethodArgumentNotValidException.asErrorList(): List<String> = this.allErrors.mapNotNull { it.defaultMessage }

data class ErrorResponse(
  val status: Int,
  val errorCode: Int? = null,
  val userMessage: String? = null,
  val developerMessage: String? = null,
  val moreInfo: String? = null,
  val errors: List<String>? = null,
) {
  constructor(
    status: HttpStatus,
    errorCode: Int? = null,
    userMessage: String? = null,
    developerMessage: String? = null,
    moreInfo: String? = null,
    errors: List<String>? = null,
  ) :
    this(status.value(), errorCode, userMessage, developerMessage, moreInfo, errors)
}

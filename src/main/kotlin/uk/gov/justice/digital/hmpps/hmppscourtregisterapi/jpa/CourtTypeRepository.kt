package uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa

import jakarta.persistence.Entity
import jakarta.persistence.Id
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CourtTypeRepository : CrudRepository<CourtType, String>

@Entity
data class CourtType(
  @Id
  val id: String,
  var description: String,
)

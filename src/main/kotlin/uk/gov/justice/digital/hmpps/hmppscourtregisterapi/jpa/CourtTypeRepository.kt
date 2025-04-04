package uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa

import jakarta.persistence.Entity
import jakarta.persistence.Id
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.client.prisonapi.dto.Agency

@Repository
interface CourtTypeRepository : CrudRepository<CourtType, String>

@Entity
data class CourtType(
  @Id
  val id: String,
  var description: String,
) {

  companion object {
    fun from(oUCodeL1Name: String): CourtType = CourtType(oUCodeL1Name.uppercase().replace(" ", ""), oUCodeL1Name)

    fun from(agency: Agency): CourtType = CourtType(agency.courtType ?: "UNK", agency.courtTypeDescription ?: "Unknown")
  }
}

package uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable

@Entity
@Immutable
@Table(name = "court_text_search")
data class CourtTextSearch(
  @Id
  val id: String,
  val plainTextSearch: String,
)

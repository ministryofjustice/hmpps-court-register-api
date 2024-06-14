package uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import org.hibernate.boot.MetadataBuilder
import org.hibernate.boot.spi.MetadataBuilderContributor
import org.hibernate.dialect.function.StandardSQLFunction
import org.hibernate.type.StandardBasicTypes

@Entity
@Immutable
@Table(name = "court_text_search")
data class CourtTextSearch(
  @Id
  val id: String,
)

class TextSearchSqlFunctionTemplate : MetadataBuilderContributor {
  override fun contribute(metadataBuilder: MetadataBuilder) {
    metadataBuilder.applySqlFunction(
      "search_court_text",
      StandardSQLFunction("textSearchVector @@ plainto_tsquery", StandardBasicTypes.BOOLEAN),
    )
  }
}

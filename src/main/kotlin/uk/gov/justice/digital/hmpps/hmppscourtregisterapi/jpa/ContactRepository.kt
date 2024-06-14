package uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface ContactRepository : CrudRepository<Contact, Int>

@Entity
@EntityListeners(AuditingEntityListener::class)
data class Contact(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int? = null,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  var building: Building,

  var type: String,
  var detail: String?,

  @CreatedDate
  @Column(nullable = false)
  var createdDatetime: LocalDateTime = LocalDateTime.MIN,
  @LastModifiedDate
  @Column(nullable = false)
  var lastUpdatedDatetime: LocalDateTime = LocalDateTime.MIN,
)

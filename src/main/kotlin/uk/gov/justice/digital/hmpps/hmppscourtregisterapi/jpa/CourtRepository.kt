package uk.gov.justice.digital.hmpps.hmppscourtregisterapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppscourtregisterapi.resource.UpdateBuildingDto
import java.time.LocalDateTime

@Repository
interface CourtRepository : PagingAndSortingRepository<Court, String>, CrudRepository<Court, String> {

  fun findByActiveOrderById(active: Boolean): List<Court>

  @Query(
    """
    select distinct c from Court c 
    where (:active is null or c.active = :active) 
    and (coalesce(:courtTypeIds) is null or c.courtType.id in (:courtTypeIds))
  """,
  )
  fun findPage(
    @Param("active") active: Boolean?,
    @Param("courtTypeIds") courtTypeId: List<String>?,
    pageable: Pageable,
  ): Page<Court>

  // Note that the `(:textSearch is null or (search_court_text(:textSearch) = true)` trick doesn't work for an SQLTemplateFunction
  // So we use a separate query if a text search has been included
  @Query(
    """
    select c from Court c where c in ( 
      select distinct c from Court c
      join CourtTextSearch ts on c.id = ts.id
      where (:active is null or c.active = :active) 
      and (coalesce(:courtTypeIds) is null or c.courtType.id in (:courtTypeIds))
      and (search_court_text(:textSearch) = true)
    )
  """,
  )
  fun findPageWithTextSearch(
    @Param("active") active: Boolean?,
    @Param("courtTypeIds") courtTypeId: List<String>?,
    @Param("textSearch") textSearch: String,
    pageable: Pageable,
  ): Page<Court>
}

@Entity
@EntityListeners(AuditingEntityListener::class)
data class Court(
  @Id
  val id: String,
  var courtName: String,
  var courtDescription: String?,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "TYPE", nullable = false)
  var courtType: CourtType,

  var active: Boolean,

  @CreatedDate
  @Column(nullable = false)
  var createdDatetime: LocalDateTime = LocalDateTime.MIN,

  @LastModifiedDate
  @Column(nullable = false)
  var lastUpdatedDatetime: LocalDateTime = LocalDateTime.MIN,

  @OneToMany(cascade = [CascadeType.ALL], mappedBy = "court", orphanRemoval = true)
  var buildings: List<Building> = listOf(),

) {
  fun addBuilding(dto: UpdateBuildingDto): Building {
    val building = Building(
      court = this,
      subCode = dto.subCode,
      buildingName = dto.buildingName,
      street = dto.street,
      locality = dto.locality,
      town = dto.town,
      postcode = dto.postcode,
      county = dto.county,
      country = dto.country,
      active = dto.active,
    )
    building.contacts = dto.contacts.map { building.addContact(it) }
    buildings = buildings.plus(building)

    return building
  }
}

package com.github.klinq.jpaspec

import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import javax.persistence.*

@Repository
interface TvShowRepository : CrudRepository<TvShow, Int>, JpaSpecificationExecutor<TvShow>

@Repository
interface GenreRepository : CrudRepository<Genre, Int>, JpaSpecificationExecutor<Genre>

interface HasName {
    val name: String
}

interface HasStars {
    val stars: Int
}

@Embeddable
data class Price(
        val amount: BigDecimal? = 0.toBigDecimal(),
        val currency: String = "EUR"
)

@Entity
data class Genre(
        @Id
        @GeneratedValue
        val id: Int = 0,
        override val name: String = "",
        @OneToMany(cascade = [CascadeType.ALL])
        val starRatings: Set<StarRating> = emptySet()
) : HasName

@Entity
data class TvShow(
        @Id
        @GeneratedValue
        val id: Int = 0,
        @ManyToOne
        val genre: Genre? = null,
        override val name: String = "",
        val synopsis: String = "",
        val availableOnNetflix: Boolean = false,
        val releaseDate: String? = null,
        @OneToMany(cascade = [CascadeType.ALL])
        val starRatings: Set<StarRating> = emptySet(),
        @Embedded
        val price: Price
) : HasName

@Entity
data class StarRating(
        @Id
        @GeneratedValue
        val id: Int = 0,
        override val stars: Int = 0) : HasStars


// Convenience functions (using the DSL) that make assembling queries more readable and allows for dynamic queries.
// Note: these functions return null for a null input. This means that when included in
// and() or or() they will be ignored as if they weren't supplied.

fun hasName(name: String?): Specification<HasName>? = name?.let {
    HasName::name.equal(it)
}

fun availableOnNetflix(available: Boolean?): Specification<TvShow>? = available?.let {
    TvShow::availableOnNetflix.equal(it)
}

fun hasReleaseDateIn(releaseDates: List<String>?): Specification<TvShow>? = releaseDates?.let {
    TvShow::releaseDate.`in`(releaseDates)
}

fun hasKeywordIn(keywords: List<String>?): Specification<TvShow>? = keywords?.let {
    or(keywords.map(::hasKeyword))
}

fun hasKeyword(keyword: String?): Specification<TvShow>? = keyword?.let {
    TvShow::synopsis.like("%$keyword%")
}

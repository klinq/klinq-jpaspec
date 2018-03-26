package com.github.klinq.jpaspec

import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.jpa.domain.Specification
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.transaction.annotation.Transactional

@RunWith(SpringRunner::class)
@SpringBootTest
@SpringBootApplication
@Transactional
@TestPropertySource(properties = ["logging.level.org.hibernate=DEBUG"])
open class KlinqJpaSpecTest {

    @Autowired
    lateinit var tvShowRepo: TvShowRepository

    @Autowired
    lateinit var genreRepo: GenreRepository

    lateinit var hemlockGrove: TvShow
    lateinit var theWalkingDead: TvShow
    lateinit var betterCallSaul: TvShow

    lateinit var crimeDrama: Genre
    lateinit var horrorThriller: Genre

    @Before
    fun setup() {
        genreRepo.apply {
            crimeDrama = save(Genre(name = "Crime drama"))
            horrorThriller = save(Genre(name = "Horror Thriller"))
        }

        tvShowRepo.apply {
            hemlockGrove = save(
                    TvShow(
                            name = "Hemlock Grove",
                            genre = horrorThriller,
                            availableOnNetflix = true,
                            synopsis = "A teenage girl is brutally murdered, sparking a hunt for her killer. But in a town where everyone hides a secret, will they find the monster among them?",
                            releaseDate = "2013"))

            theWalkingDead = save(
                    TvShow(
                            name = "The Walking Dead",
                            availableOnNetflix = false,
                            synopsis = "Sheriff Deputy Rick Grimes leads a group of survivors in a world overrun by the walking dead. Fighting the dead, fearing the living.",
                            releaseDate = "2010",
                            starRatings = setOf(StarRating(stars = 3), StarRating(stars = 4))))

            betterCallSaul = save(
                    TvShow(
                            name = "Better Call Saul",
                            genre = crimeDrama,
                            availableOnNetflix = false,
                            synopsis = "The trials and tribulations of criminal lawyer, Jimmy McGill, in the time leading up to establishing his strip-mall law office in Albuquerque, New Mexico.",
                            starRatings = setOf(StarRating(stars = 4), StarRating(stars = 2))))
        }
    }

    @After
    fun tearDown() {
        tvShowRepo.deleteAll()
    }

    /**
     * A TV show query DTO - typically used at the service layer.
     */
    data class TvShowQuery(
            val name: String? = null,
            val availableOnNetflix: Boolean? = null,
            val keywords: List<String> = listOf(),
            val releaseDates: List<String> = listOf()
    )

    /**
     * A single TvShowQuery is equivalent to an AND of all supplied criteria.
     * Note: any criteria that is null will be ignored (not included in the query).
     */
    fun TvShowQuery.toSpecification(): Specification<TvShow> = and(
            hasName(name),
            availableOnNetflix(availableOnNetflix),
            hasKeywordIn(keywords),
            hasReleaseDateIn(releaseDates)
    )

    /**
     * A collection of TvShowQueries is equivalent to an OR of all the queries in the collection.
     */
    fun Iterable<TvShowQuery>.toSpecification(): Specification<TvShow> = or(
            map { query -> query.toSpecification() }
    )

    @Test
    fun `Get a tv show by id`() {
        assertThat(tvShowRepo.findById(hemlockGrove.id)).isEqualTo(hemlockGrove)
    }

    @Test
    fun `Get a tv show by id equality`() {
        assertThat(tvShowRepo.findOne(TvShow::id.equal(theWalkingDead.id))).isEqualTo(theWalkingDead)
    }

    @Test
    fun `Get tv shows by id notEqual`() {
        assertThat(tvShowRepo.findAll(TvShow::name.notEqual(theWalkingDead.name))).containsOnly(betterCallSaul, hemlockGrove)
    }

    @Test
    fun `Get tv show by id in`() {
        assertThat(tvShowRepo.findAll(TvShow::id.`in`(setOf(hemlockGrove.id, theWalkingDead.id)))).containsOnly(hemlockGrove, theWalkingDead)
    }

    @Test
    fun `Get tv show by id lt`() {
        assertThat(tvShowRepo.findAll(TvShow::id.lt(betterCallSaul.id))).containsOnly(hemlockGrove, theWalkingDead)
    }

    @Test
    fun `Get tv show by id le`() {
        assertThat(tvShowRepo.findAll(TvShow::id.le(theWalkingDead.id))).containsOnly(hemlockGrove, theWalkingDead)
    }

    @Test
    fun `Get tv show by id gt`() {
        assertThat(tvShowRepo.findAll(TvShow::id.gt(hemlockGrove.id))).containsOnly(theWalkingDead, betterCallSaul)
    }

    @Test
    fun `Get tv show by id ge`() {
        assertThat(tvShowRepo.findAll(TvShow::id.ge(theWalkingDead.id))).containsOnly(theWalkingDead, betterCallSaul)
    }

    @Test
    fun `Get tv show by name lessThan`() {
        assertThat(tvShowRepo.findAll(TvShow::name.lessThan("C"))).containsOnly(betterCallSaul)
    }

    @Test
    fun `Get tv show by name lessThanOrEqualTo`() {
        assertThat(tvShowRepo.findAll(TvShow::name.lessThanOrEqualTo("Hemlock Grove"))).containsOnly(betterCallSaul, hemlockGrove)
    }

    @Test
    fun `Get tv show by name greaterThan`() {
        assertThat(tvShowRepo.findAll(TvShow::name.greaterThan("Hemlock Grove"))).containsOnly(theWalkingDead)
    }

    @Test
    fun `Get tv show by name greaterThanOrEqualTo`() {
        assertThat(tvShowRepo.findAll(TvShow::name.greaterThanOrEqualTo("Hemlock Grove"))).containsOnly(hemlockGrove, theWalkingDead)
    }

    @Test
    fun `Get tv show by name between`() {
        assertThat(tvShowRepo.findAll(TvShow::name.between("A", "H"))).containsOnly(betterCallSaul)
    }

    @Test
    fun `Get tv show by boolean isTrue`() {
        assertThat(tvShowRepo.findAll(TvShow::availableOnNetflix.isTrue())).containsOnly(hemlockGrove)
    }

    @Test
    fun `Get tv show by boolean isFalse`() {
        assertThat(tvShowRepo.findAll(TvShow::availableOnNetflix.isFalse())).containsOnly(betterCallSaul, theWalkingDead)
    }

    @Test
    fun `Get tv show by releaseDate isNull`() {
        assertThat(tvShowRepo.findAll(TvShow::releaseDate.isNull())).containsOnly(betterCallSaul)
    }

    @Test
    fun `Get tv show by releaseDate isNotNull`() {
        assertThat(tvShowRepo.findAll(TvShow::releaseDate.isNotNull())).containsOnly(hemlockGrove, theWalkingDead)
    }

    @Test
    fun `Get tv show by ratings isEmpty`() {
        assertThat(tvShowRepo.findAll(TvShow::starRatings.isEmpty())).containsOnly(hemlockGrove)
    }

    @Test
    fun `Get tv show by ratings isNotEmpty`() {
        assertThat(tvShowRepo.findAll(TvShow::starRatings.isNotEmpty())).containsOnly(betterCallSaul, theWalkingDead)
    }

    @Test
    fun `Get tv show by isMember`() {
        assertThat(tvShowRepo.findAll(TvShow::starRatings.isMember(theWalkingDead.starRatings.first()))).containsOnly(theWalkingDead)
    }

    @Test
    fun `Get tv show by isNotMember`() {
        assertThat(tvShowRepo.findAll(TvShow::starRatings.isNotMember(betterCallSaul.starRatings.first()))).containsOnly(theWalkingDead, hemlockGrove)
    }

    @Test
    fun `Get a tv show by name like`() {
        assertThat(tvShowRepo.findAll(TvShow::name.like("The%"))).containsOnly(theWalkingDead)
    }

    @Test
    fun `Get a tv show by synopsis like with escape char`() {
        assertThat(tvShowRepo.findAll(TvShow::synopsis.like("%them\\?", escapeChar = '\\'))).containsOnly(hemlockGrove)
    }

    @Test
    fun `Get a tv show by name notLike`() {
        assertThat(tvShowRepo.findAll(TvShow::name.notLike("The %"))).containsOnly(betterCallSaul, hemlockGrove)
    }

    @Test
    fun `Get a tv show by synopsis notLike with escape char`() {
        assertThat(tvShowRepo.findAll(TvShow::synopsis.notLike("%\\.", escapeChar = '\\'))).containsOnly(hemlockGrove)
    }

    @Test
    fun `Find tv shows with and`() {
        assertThat(tvShowRepo.findAll(TvShow::availableOnNetflix.isFalse() and TvShow::releaseDate.equal("2010"))).containsOnly(theWalkingDead)
    }

    @Test
    fun `Find tv shows with or`() {
        assertThat(tvShowRepo.findAll(TvShow::availableOnNetflix.isTrue() or TvShow::releaseDate.equal("2010"))).containsOnly(hemlockGrove, theWalkingDead)
    }

    @Test
    fun `Find tv shows with not operator`() {
        assertThat(tvShowRepo.findAll(!TvShow::releaseDate.equal("2010"))).containsOnly(hemlockGrove)
    }

    @Test
    fun `Test Join`() {
        assertThat(tvShowRepo.findAll(TvShow::genre.toJoin().where(Genre::name).equal("Crime drama"))).containsOnly(betterCallSaul)
    }

    @Test
    fun `Test Left Join`() {
        val spec = TvShow::genre.toLeftJoin().where(Genre::name).equal("Crime drama") or
                TvShow::genre.toLeftJoin().where(Genre::id).isNull()
        assertThat(tvShowRepo.findAll(spec)).containsOnly(betterCallSaul, theWalkingDead)
    }

    @Test
    fun `Test Collection Join`() {
        assertThat(tvShowRepo.findAll(TvShow::starRatings.toCollectionJoin().where(StarRating::stars).equal(2))).containsOnly(betterCallSaul)
    }

    @Test
    fun `Test Collection Left Join`() {
        val spec = TvShow::starRatings.toCollectionLeftJoin().where(StarRating::stars).notIn(listOf(2, 4)) or
                TvShow::starRatings.toCollectionLeftJoin().where(StarRating::id).isNull()
        assertThat(tvShowRepo.findAll(spec)).containsOnly(theWalkingDead, hemlockGrove)
    }

    @Test
    fun `Find tv shows by query DTO`() {
        val query = TvShowQuery(availableOnNetflix = false, keywords = listOf("Rick", "Jimmy"))
        assertThat(tvShowRepo.findAll(query.toSpecification())).containsOnly(betterCallSaul, theWalkingDead)
    }

    @Test
    fun `Find tv shows by query DTO - empty query`() {
        val query = TvShowQuery()
        assertThat(tvShowRepo.findAll(query.toSpecification())).containsOnly(betterCallSaul, hemlockGrove, theWalkingDead)
    }

    @Test
    fun `Find tv shows by multiple query DTOs`() {
        val queries = listOf(
                TvShowQuery(availableOnNetflix = false, keywords = listOf("Jimmy")),
                TvShowQuery(availableOnNetflix = true, keywords = listOf("killer", "monster"), releaseDates = listOf("2010", "2013"))
        )
        assertThat(tvShowRepo.findAll(queries.toSpecification())).containsOnly(betterCallSaul, hemlockGrove)
    }

    @Test
    fun `Find tv shows by empty query DTOs list`() {
        val queries = listOf<TvShowQuery>()
        assertThat(tvShowRepo.findAll(queries.toSpecification())).containsOnly(betterCallSaul, hemlockGrove, theWalkingDead)
    }

    @Test
    fun `Find tv shows by inlined query`() {
        val shows = tvShowRepo.findAll(and(
                availableOnNetflix(false),
                hasKeywordIn(listOf("Rick", "Jimmy"))
        ))
        assertThat(shows).containsOnly(betterCallSaul, theWalkingDead)
    }

    @Test
    fun `Find tv shows by complex inlined query`() {
        val shows = tvShowRepo.findAll(
                or(
                        and(
                                availableOnNetflix(false),
                                hasKeywordIn(listOf("Jimmy"))
                        ),
                        and(
                                availableOnNetflix(true),
                                or(
                                        hasKeyword("killer"),
                                        hasKeyword("monster")
                                )
                        )
                )
        )
        assertThat(shows).containsOnly(betterCallSaul, hemlockGrove)
    }

}

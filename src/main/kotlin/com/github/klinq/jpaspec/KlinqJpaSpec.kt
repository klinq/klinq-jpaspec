package com.github.klinq.jpaspec

import org.springframework.data.jpa.domain.Specification
import javax.persistence.criteria.*
import kotlin.reflect.KProperty1

// Version of Specification.where that makes the CriteriaBuilder implicit
private fun <T> where(distinct: Boolean, makePredicate: CriteriaBuilder.(Root<T>) -> Predicate): Specification<T> =
        Specification.where<T> { root, criteriaQuery, criteriaBuilder ->
            criteriaQuery.distinct(distinct)
            criteriaBuilder.makePredicate(root)
        }

class WhereBuilder<T, R>(private val distinct: Boolean, private val path: (Root<T>) -> Path<R>) {
    fun spec(makePredicate: CriteriaBuilder.(Path<R>) -> Predicate): Specification<T> =
            where(distinct) { root -> makePredicate(path(root)) }
}

class FromBuilder<Z, out T>(private val from: (Root<Z>) -> From<Z, T>) {
    fun <R> where(prop: KProperty1<in T, R?>, distinct: Boolean = false): WhereBuilder<Z, R?> = WhereBuilder(distinct) { from(it).get<R>(prop.name) }

    fun <R> join(prop: KProperty1<in T, R?>, joinType: JoinType = JoinType.INNER): FromBuilder<Z, R> =
            FromBuilder { from(it).join(prop.name, joinType) }

    fun <R> leftJoin(prop: KProperty1<in T, R?>): FromBuilder<Z, R> =
            FromBuilder { from(it).join(prop.name, JoinType.LEFT) }

    fun <R> joinCollection(prop: KProperty1<in T, Collection<R>>, joinType: JoinType = JoinType.INNER): FromBuilder<Z, R> =
            FromBuilder { from(it).join(prop.name, joinType) }

    fun <R> leftJoinCollection(prop: KProperty1<in T, Collection<R>>): FromBuilder<Z, R> =
            FromBuilder { from(it).join(prop.name, JoinType.LEFT) }
}

fun <T, R> KProperty1<in T, R>.toWhere(distinct: Boolean = false): WhereBuilder<T, R> = WhereBuilder(distinct) { it.get(this.name) }
fun <Z> from() = FromBuilder<Z, Z> { it }

fun <Z, R> KProperty1<in Z, R?>.toJoin(): FromBuilder<Z, R> = from<Z>().join(this)
fun <Z, R> KProperty1<in Z, R?>.toLeftJoin(): FromBuilder<Z, R> = from<Z>().leftJoin(this)

fun <Z, R> KProperty1<in Z, Collection<R>>.toCollectionJoin(): FromBuilder<Z, R> = from<Z>().joinCollection(this)
fun <Z, R> KProperty1<in Z, Collection<R>>.toCollectionLeftJoin(): FromBuilder<Z, R> = from<Z>().leftJoinCollection(this)

// Equality
fun <T, R> KProperty1<in T, R?>.equal(x: R): Specification<T> = toWhere().equal(x)

fun <T, R> WhereBuilder<T, R?>.equal(x: R): Specification<T> = spec { equal(it, x) }

fun <T, R> KProperty1<in T, R?>.notEqual(x: R): Specification<T> = toWhere().notEqual(x)
fun <T, R> WhereBuilder<T, R?>.notEqual(x: R): Specification<T> = spec { notEqual(it, x) }

//In
fun <T, R> KProperty1<in T, R?>.`in`(values: Collection<R>): Specification<T> = toWhere().`in`(values)

fun <T, R> WhereBuilder<T, R?>.`in`(values: Collection<R>): Specification<T> = createIn(values) { it }

fun <T, R> KProperty1<in T, R?>.notIn(values: Collection<R>): Specification<T> = toWhere().notIn(values)
fun <T, R> WhereBuilder<T, R?>.notIn(values: Collection<R>): Specification<T> = createIn(values) { it.not() }

private fun <R, T> WhereBuilder<T, R?>.createIn(values: Collection<R>, post: (Predicate) -> Predicate): Specification<T> =
        if (values.isNotEmpty()) {
            spec { path ->
                `in`(path)
                        .also { value -> values.forEach { value.value(it) } }
                        .let { post(it) }
            }
        } else {
            //SQL cannot handle empty in queries (at least some DBs), so default to false
            Specification { _, _, criteriaBuilder ->
                org.hibernate.query.criteria.internal.predicate.BooleanStaticAssertionPredicate(
                        criteriaBuilder as org.hibernate.query.criteria.internal.CriteriaBuilderImpl?, false)
            }
        }

// Comparison
fun <T> KProperty1<in T, Number?>.le(x: Number) = toWhere().le(x)

fun <T> WhereBuilder<T, Number?>.le(x: Number) = spec { le(it, x) }

fun <T> KProperty1<in T, Number?>.lt(x: Number) = toWhere().lt(x)
fun <T> WhereBuilder<T, Number?>.lt(x: Number) = spec { lt(it, x) }

fun <T> KProperty1<in T, Number?>.ge(x: Number) = toWhere().ge(x)
fun <T> WhereBuilder<T, Number?>.ge(x: Number) = spec { ge(it, x) }

fun <T> KProperty1<in T, Number?>.gt(x: Number) = toWhere().gt(x)
fun <T> WhereBuilder<T, Number?>.gt(x: Number) = spec { gt(it, x) }

fun <T, R : Comparable<R>> KProperty1<in T, R?>.lessThan(x: R) = toWhere().lessThan(x)
fun <T, R : Comparable<R>> WhereBuilder<T, R?>.lessThan(x: R) = spec { lessThan<R>(it, x) }

fun <T, R : Comparable<R>> KProperty1<in T, R?>.lessThanOrEqualTo(x: R) = toWhere().lessThanOrEqualTo(x)
fun <T, R : Comparable<R>> WhereBuilder<T, R?>.lessThanOrEqualTo(x: R) = spec { lessThanOrEqualTo<R>(it, x) }

fun <T, R : Comparable<R>> KProperty1<in T, R?>.greaterThan(x: R) = toWhere().greaterThan(x)
fun <T, R : Comparable<R>> WhereBuilder<T, R?>.greaterThan(x: R) = spec { greaterThan<R>(it, x) }

fun <T, R : Comparable<R>> KProperty1<in T, R?>.greaterThanOrEqualTo(x: R) = toWhere().greaterThanOrEqualTo(x)
fun <T, R : Comparable<R>> WhereBuilder<T, R?>.greaterThanOrEqualTo(x: R) = spec { greaterThanOrEqualTo<R>(it, x) }

fun <T, R : Comparable<R>> KProperty1<in T, R?>.between(x: R, y: R) = toWhere().between(x, y)
fun <T, R : Comparable<R>> WhereBuilder<T, R?>.between(x: R, y: R) = spec { between<R>(it, x, y) }

// True/False
fun <T> KProperty1<in T, Boolean?>.isTrue() = toWhere().isTrue()

fun <T> WhereBuilder<T, Boolean?>.isTrue() = spec { isTrue(it) }

fun <T> KProperty1<in T, Boolean?>.isFalse() = toWhere().isFalse()
fun <T> WhereBuilder<T, Boolean?>.isFalse() = spec { isFalse(it) }

// Null / NotNull
fun <T, R> KProperty1<in T, R?>.isNull() = toWhere().isNull()

fun <T, R> WhereBuilder<T, R?>.isNull() = spec { isNull(it) }

fun <T, R> KProperty1<in T, R?>.isNotNull() = toWhere().isNotNull()
fun <T, R> WhereBuilder<T, R?>.isNotNull() = spec { isNotNull(it) }

// Collections
fun <T, R : Collection<*>> KProperty1<in T, R?>.isEmpty() = toWhere().isEmpty()

fun <T, R : Collection<*>> WhereBuilder<T, R?>.isEmpty() = spec { isEmpty(it) }

fun <T, R : Collection<*>> KProperty1<in T, R?>.isNotEmpty() = toWhere().isNotEmpty()
fun <T, R : Collection<*>> WhereBuilder<T, R?>.isNotEmpty() = spec { isNotEmpty(it) }

fun <T, E, R : Collection<E>> KProperty1<in T, R?>.isMember(elem: E) = toWhere().isMember(elem)
fun <T, E, R : Collection<E>> WhereBuilder<T, R?>.isMember(elem: E) = spec { isMember(elem, it) }

fun <T, E, R : Collection<E>> KProperty1<in T, R?>.isNotMember(elem: E) = toWhere().isNotMember(elem)
fun <T, E, R : Collection<E>> WhereBuilder<T, R?>.isNotMember(elem: E) = spec { isNotMember(elem, it) }

// Strings
fun <T> KProperty1<in T, String?>.like(x: String): Specification<T> = toWhere().like(x)

fun <T> WhereBuilder<T, String?>.like(x: String): Specification<T> = spec { like(it, x) }

fun <T> KProperty1<in T, String?>.likeLower(x: String): Specification<T> = toWhere().likeLower(x)
fun <T> WhereBuilder<T, String?>.likeLower(x: String): Specification<T> = spec { like(lower(it), x.toLowerCase()) }

fun <T> KProperty1<in T, String?>.like(x: String, escapeChar: Char): Specification<T> = toWhere().like(x, escapeChar)
fun <T> WhereBuilder<T, String?>.like(x: String, escapeChar: Char): Specification<T> = spec { like(it, x, escapeChar) }

fun <T> KProperty1<in T, String?>.notLike(x: String): Specification<T> = toWhere().notLike(x)
fun <T> WhereBuilder<T, String?>.notLike(x: String): Specification<T> = spec { notLike(it, x) }

fun <T> KProperty1<in T, String?>.notLike(x: String, escapeChar: Char): Specification<T> = toWhere().notLike(x, escapeChar)
fun <T> WhereBuilder<T, String?>.notLike(x: String, escapeChar: Char): Specification<T> = spec { notLike(it, x, escapeChar) }

// And
infix fun <T> Specification<T>?.and(other: Specification<in T>?): Specification<T> = and(listOf(this, other))

fun <T> and(vararg specs: Specification<in T>?): Specification<T> {
    return and(specs.toList())
}

fun <T> and(specs: Iterable<Specification<in T>?>): Specification<T> {
    return combineSpecification(specs, Specification<T>::and)
}

// Or
infix fun <T> Specification<T>?.or(other: Specification<in T>?): Specification<T> = or(listOf(this, other))

fun <T> or(vararg specs: Specification<in T>?): Specification<T> {
    return or(specs.toList())
}

fun <T> or(specs: Iterable<Specification<in T>?>): Specification<T> {
    return combineSpecification(specs, Specification<T>::or)
}

// Not
@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
operator fun <T> Specification<T>?.not(): Specification<T> = Specification.not(this)

// Combines Specification with an operation
private fun <T> combineSpecification(specs: Iterable<Specification<in T>?>, operation: Specification<T>.(Specification<T>) -> Specification<T>): Specification<T> {
    return specs.filterNotNull().fold(emptySpecification()) { existing, new ->
        @Suppress("UNCHECKED_CAST")
        existing.operation(new as Specification<T>)
    }
}

// Empty Specification
@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
private fun <T> emptySpecification(): Specification<T> = Specification.where<T>(null)
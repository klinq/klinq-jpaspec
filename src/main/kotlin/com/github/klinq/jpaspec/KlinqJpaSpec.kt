package com.github.klinq.jpaspec

import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.domain.Specifications
import javax.persistence.criteria.*
import kotlin.reflect.KProperty1

// Version of Specifications.where that makes the CriteriaBuilder implicit
private fun <T> where(makePredicate: CriteriaBuilder.(Root<T>) -> Predicate): Specifications<T> =
        Specifications.where<T> { root, _, criteriaBuilder -> criteriaBuilder.makePredicate(root) }

class WhereBuilder<T, R>(private val path: (Root<T>) -> Path<R>) {
    @Suppress("UNCHECKED_CAST")
    fun spec(makePredicate: CriteriaBuilder.(Path<R>) -> Predicate): Specifications<T> =
            where { root -> makePredicate(path(root)) }

    // Equality
    fun equal(x: R): Specifications<T> = spec { equal(it, x) }

    fun notEqual(x: R): Specifications<T> = spec { notEqual(it, x) }

    // Ignores empty collection otherwise an empty 'in' predicate will be generated which will never match any results
    fun `in`(values: Collection<R>): Specifications<T> =
            if (values.isNotEmpty()) {
                spec { path ->
                    `in`(path).also { value -> values.forEach { value.value(it) } }
                }
            } else {
                Specifications.where<T>(null)
            }

    fun notIn(values: Collection<R>): Specifications<T> =
            if (values.isNotEmpty()) {
                spec { path ->
                    `in`(path).also { value -> values.forEach { value.value(it) } }.not()
                }
            } else {
                Specifications.where<T>(null)
            }

    // Null / NotNull
    fun isNull() = spec { isNull(it) }

    fun isNotNull() = spec { isNotNull(it) }
}

class FromBuilder<Z, T>(val from: (Root<Z>) -> From<Z, T>) {
    inline fun <reified R> where(prop: KProperty1<T, R?>): WhereBuilder<Z, R> = WhereBuilder({ from(it).get<R>(prop.name) })

    inline fun <reified R> join(prop: KProperty1<T, R?>, joinType: JoinType = JoinType.INNER): FromBuilder<Z, R> =
            FromBuilder({ from(it).join(prop.name, joinType) })
    inline fun <reified R> leftJoin(prop: KProperty1<T, R?>): FromBuilder<Z, R> = FromBuilder({ from(it).join(prop.name, JoinType.LEFT) })

    inline fun <reified R> joinCollection(prop: KProperty1<T, Collection<R>>, joinType: JoinType = JoinType.INNER): FromBuilder<Z, R> =
            FromBuilder({ from(it).join(prop.name, joinType) })
    inline fun <reified R> leftJoinCollection(prop: KProperty1<T, Collection<R>>): FromBuilder<Z, R> = FromBuilder({ from(it).join(prop.name, JoinType.LEFT) })
}

fun <T, R> KProperty1<T, R>.toWhere(): WhereBuilder<T, R> = WhereBuilder({ it.get(this.name) })
fun <Z> from() = FromBuilder<Z, Z>({ it })

inline fun <reified Z, reified R> KProperty1<Z, R?>.toJoin(): FromBuilder<Z, R> = from<Z>().join(this)
inline fun <reified Z, reified R> KProperty1<Z, R?>.toLeftJoin(): FromBuilder<Z, R> = from<Z>().leftJoin(this)

inline fun <reified Z, reified R> KProperty1<Z, Collection<R>>.toCollectionJoin(): FromBuilder<Z, R> = from<Z>().joinCollection(this)
inline fun <reified Z, reified R> KProperty1<Z, Collection<R>>.toCollectionLeftJoin(): FromBuilder<Z, R> = from<Z>().leftJoinCollection(this)

// Equality
fun <T, R> KProperty1<T, R?>.equal(x: R): Specifications<T> = toWhere().equal(x)
fun <T, R> KProperty1<T, R?>.notEqual(x: R): Specifications<T> = toWhere().notEqual(x)

fun <T, R : Any> KProperty1<T, R?>.`in`(values: Collection<R>): Specifications<T> = toWhere().`in`(values)
fun <T, R : Any> KProperty1<T, R?>.notIn(values: Collection<R>): Specifications<T> = toWhere().notIn(values)

// Comparison
fun <T> KProperty1<T, Number?>.le(x: Number) = toWhere().le(x)
fun <T> WhereBuilder<T, Number?>.le(x: Number) = spec { le(it, x) }

fun <T> KProperty1<T, Number?>.lt(x: Number) = toWhere().lt(x)
fun <T> WhereBuilder<T, Number?>.lt(x: Number) = spec { lt(it, x) }

fun <T> KProperty1<T, Number?>.ge(x: Number) = toWhere().ge(x)
fun <T> WhereBuilder<T, Number?>.ge(x: Number) = spec { ge(it, x) }

fun <T> KProperty1<T, Number?>.gt(x: Number) = toWhere().gt(x)
fun <T> WhereBuilder<T, Number?>.gt(x: Number) = spec { gt(it, x) }

fun <T, R : Comparable<R>> KProperty1<T, R?>.lessThan(x: R) = toWhere().lessThan(x)
@Suppress("UNCHECKED_CAST")
fun <T, R : Comparable<R>> WhereBuilder<T, R?>.lessThan(x: R) = spec { lessThan(it as Path<R>, x) }

fun <T, R : Comparable<R>> KProperty1<T, R?>.lessThanOrEqualTo(x: R) = toWhere().lessThanOrEqualTo(x)
@Suppress("UNCHECKED_CAST")
fun <T, R : Comparable<R>> WhereBuilder<T, R?>.lessThanOrEqualTo(x: R) = spec { lessThanOrEqualTo(it as Path<R>, x) }

fun <T, R : Comparable<R>> KProperty1<T, R?>.greaterThan(x: R) = toWhere().greaterThan(x)
@Suppress("UNCHECKED_CAST")
fun <T, R : Comparable<R>> WhereBuilder<T, R?>.greaterThan(x: R) = spec { greaterThan(it as Path<R>, x) }

fun <T, R : Comparable<R>> KProperty1<T, R?>.greaterThanOrEqualTo(x: R) = toWhere().greaterThanOrEqualTo(x)
@Suppress("UNCHECKED_CAST")
fun <T, R : Comparable<R>> WhereBuilder<T, R?>.greaterThanOrEqualTo(x: R) = spec { greaterThanOrEqualTo(it as Path<R>, x) }

fun <T, R : Comparable<R>> KProperty1<T, R?>.between(x: R, y: R) = toWhere().between(x, y)
@Suppress("UNCHECKED_CAST")
fun <T, R : Comparable<R>> WhereBuilder<T, R?>.between(x: R, y: R) = spec { between(it as Path<R>, x, y) }

// True/False
fun <T> KProperty1<T, Boolean?>.isTrue() = toWhere().isTrue()

fun <T> WhereBuilder<T, Boolean?>.isTrue() = spec { isTrue(it) }

fun <T> KProperty1<T, Boolean?>.isFalse() = toWhere().isFalse()
fun <T> WhereBuilder<T, Boolean?>.isFalse() = spec { isFalse(it) }

// Null / NotNull
fun <T, R> KProperty1<T, R?>.isNull() = toWhere().isNull()

fun <T, R> KProperty1<T, R?>.isNotNull() = toWhere().isNotNull()

// Collections
fun <T, R : Collection<*>> KProperty1<T, R?>.isEmpty() = toWhere().isEmpty()

fun <T, R : Collection<*>> WhereBuilder<T, R?>.isEmpty() = spec { isEmpty(it) }

fun <T, R : Collection<*>> KProperty1<T, R?>.isNotEmpty() = toWhere().isNotEmpty()
fun <T, R : Collection<*>> WhereBuilder<T, R?>.isNotEmpty() = spec { isNotEmpty(it) }

fun <T, E, R : Collection<E>> KProperty1<T, R?>.isMember(elem: E) = toWhere().isMember(elem)
fun <T, E, R : Collection<E>> WhereBuilder<T, R?>.isMember(elem: E) = spec { isMember(elem, it) }

fun <T, E, R : Collection<E>> KProperty1<T, R?>.isNotMember(elem: E) = toWhere().isNotMember(elem)
fun <T, E, R : Collection<E>> WhereBuilder<T, R?>.isNotMember(elem: E) = spec { isNotMember(elem, it) }

// Strings
fun <T> KProperty1<T, String?>.like(x: String): Specifications<T> = toWhere().like(x)

fun <T> WhereBuilder<T, String?>.like(x: String): Specifications<T> = spec { like(it, x) }

fun <T> KProperty1<T, String?>.likeLower(x: String): Specifications<T> = toWhere().likeLower(x)
fun <T> WhereBuilder<T, String?>.likeLower(x: String): Specifications<T> = spec { like(lower(it), x.toLowerCase()) }

fun <T> KProperty1<T, String?>.like(x: String, escapeChar: Char): Specifications<T> = toWhere().like(x, escapeChar)
fun <T> WhereBuilder<T, String?>.like(x: String, escapeChar: Char): Specifications<T> = spec { like(it, x, escapeChar) }

fun <T> KProperty1<T, String?>.notLike(x: String): Specifications<T> = toWhere().notLike(x)
fun <T> WhereBuilder<T, String?>.notLike(x: String): Specifications<T> = spec { notLike(it, x) }

fun <T> KProperty1<T, String?>.notLike(x: String, escapeChar: Char): Specifications<T> = toWhere().notLike(x, escapeChar)
fun <T> WhereBuilder<T, String?>.notLike(x: String, escapeChar: Char): Specifications<T> = spec { notLike(it, x, escapeChar) }

// And
@Suppress("UNCHECKED_CAST")
infix fun <T> Specifications<T>.and(other: Specification<in T>): Specifications<T> = this.and(other as Specification<T>)

inline fun <reified T> and(vararg specs: Specifications<in T>?): Specifications<T> {
    return and(specs.toList())
}

inline fun <reified T> and(specs: Iterable<Specifications<in T>?>): Specifications<T> {
    return combineSpecifications(specs, Specifications<T>::and)
}

// Or
@Suppress("UNCHECKED_CAST")
infix fun <T> Specifications<T>.or(other: Specification<in T>): Specifications<T> = this.or(other as Specification<T>)

inline fun <reified T> or(vararg specs: Specifications<in T>?): Specifications<T> {
    return or(specs.toList())
}

inline fun <reified T> or(specs: Iterable<Specifications<in T>?>): Specifications<T> {
    return combineSpecifications(specs, Specifications<T>::or)
}

// Not
operator fun <T> Specifications<T>.not(): Specifications<T> = Specifications.not(this)

// Combines Specifications with an operation
inline fun <reified T> combineSpecifications(specs: Iterable<Specification<in T>?>, operation: Specifications<T>.(Specification<T>) -> Specifications<T>): Specifications<T> {
    return specs.filterNotNull().fold(emptySpecification()) { existing, new ->
        @Suppress("UNCHECKED_CAST")
        existing.operation(new as Specification<T>)
    }
}

// Empty Specification
inline fun <reified T> emptySpecification(): Specifications<T> = Specifications.where<T>(null)
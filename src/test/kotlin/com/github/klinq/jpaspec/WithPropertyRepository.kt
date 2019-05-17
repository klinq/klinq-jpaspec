package com.github.klinq.jpaspec

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import javax.persistence.*

interface PropertyRepository : JpaRepository<Property, Int>

@Entity
@Table(name = "Property")
class Property(@ManyToOne(optional = false) val withProperty: WithProperty,
               @Column(unique = true) val key: String,
               val value: String) {
    @Id
    @GeneratedValue
    val id: Int = 0
}

interface WithPropertyRepository : JpaRepository<WithProperty, Int>, JpaSpecificationExecutor<WithProperty>

@Entity
@Table(name = "WithProperty")
class WithProperty {
    @Id
    @GeneratedValue
    val id: Int = 0

    @OneToMany
    val properties: MutableMap<String, Property> = mutableMapOf()
}
package com.github.klinq.jpaspec

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import javax.persistence.*

interface ShapeRepository : JpaRepository<Shape, Int>, JpaSpecificationExecutor<Shape>

@Entity
@Table(name = "Shape")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "DISCRIMINATOR", length = 128)
abstract class Shape {

    @Id
    @GeneratedValue
    val id: Int = 0
}

@Entity
@DiscriminatorValue("Circle")
class Circle(val diameter: Float) : Shape()

@Entity
@DiscriminatorValue("Square")
class Square(val a: Float) : Shape()
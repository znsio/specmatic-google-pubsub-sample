package com.example.order

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ProductServiceApplication

fun main(args: Array<String>) {
    SpringApplication.run(ProductServiceApplication::class.java, *args)
}

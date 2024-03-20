package com.example.product

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ProductServiceApplication @Autowired constructor( private val productService: OrderService) {
    fun run() {
        val logger = LoggerFactory.getLogger(ProductServiceApplication::class.java)
        logger.info("Starting Google PubSub Product Service...")
        productService.run()
    }
}

fun main(args: Array<String>) {
    val context = runApplication<ProductServiceApplication>(*args)
    context.getBean(ProductServiceApplication::class.java).run()
}

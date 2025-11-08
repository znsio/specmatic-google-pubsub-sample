package com.example.order

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDateTime.now

data class Order(
    val id: Int,
    val orderItems: List<OrderItem> = emptyList(),
    val lastUpdatedDate: String = now().toString(),
    val status: OrderStatus
) {
    fun totalAmount(): BigDecimal {
        return orderItems.sumOf { it.price * BigDecimal(it.quantity) }
    }
}

data class OrderItem(
    @JsonProperty("id")
    val id: Int,
    @JsonProperty("name")
    val name: String,
    @JsonProperty("quantity")
    val quantity: Int,
    @JsonProperty("price")
    val price: BigDecimal
)

data class OrderId(
    @JsonProperty("id")
    val id: Int
)
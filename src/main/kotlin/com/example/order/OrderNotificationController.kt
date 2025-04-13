package com.example.order

import org.springframework.web.bind.annotation.*
import org.springframework.http.ResponseEntity

@RestController
@RequestMapping("/order")
class OrderNotificationController(
    private val orderNotificationService: OrderNotificationService
) {

    @PostMapping("/notify")
    fun notifyOrder(@RequestBody request: NotifyOrderRequest): ResponseEntity<String> {
        println("[order-notification-controller] Received notify request: $request")
        orderNotificationService.notify(request)

        return ResponseEntity.ok("Notification triggered.")
    }
}

data class NotifyOrderRequest(
    val orderId: Int,
    val event: OrderEvent,
    val timestamp: String
)

enum class OrderEvent {
    ORDER_PROCESSED,
    ORDER_CANCELLED
}

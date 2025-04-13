package com.example.order

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service

private const val SERVICE_NAME = "order-notification-service"

@Service
class OrderNotificationService(
    private val config: Configuration
) {

    private val googlePubSubClient = GooglePubSubClient(config.projectId, SERVICE_NAME)

    fun notify(request: NotifyOrderRequest) {
        println("[$SERVICE_NAME] Publishing the notify message on topic 'notify-order'..")
        googlePubSubClient.publish(
            "notify-order",
            ObjectMapper().writeValueAsString(request)
        )
    }
}

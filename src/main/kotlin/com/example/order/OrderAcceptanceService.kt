package com.example.order

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service

private const val SERVICE_NAME = "OrderAcceptanceService"
private const val TOPIC_NAME = "accepted-orders"

@Service
class OrderAcceptanceService(
    config: Configuration
) {
    private val googlePubSubClient = GooglePubSubClient(config.projectId, SERVICE_NAME)

    fun notify(request: OrderUpdateRequest) {
        println("[$SERVICE_NAME] Publishing the acceptance message on topic '$TOPIC_NAME'..")
        googlePubSubClient.publish(
            TOPIC_NAME,
            ObjectMapper().writeValueAsString(request)
        )
    }
}
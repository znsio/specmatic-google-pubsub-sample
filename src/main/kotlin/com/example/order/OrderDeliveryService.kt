package com.example.order

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.cloud.pubsub.v1.AckReplyConsumer
import com.google.pubsub.v1.ProjectSubscriptionName
import com.google.pubsub.v1.ProjectTopicName
import com.google.pubsub.v1.PubsubMessage
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Service

private const val SERVICE_NAME = "OrderDeliveryService"
private const val ORDER_OUT_FOR_DELIVERY_TOPIC = "out-for-delivery-orders"
private const val ORDER_DELIVERY_SERVICE_SUBSCRIPTION_PREFIX = "orderdeliveryservice-subscription"

@Service
class OrderDeliveryService(
    private val orderRepository: OrderRepository,
    private val config: Configuration
): ApplicationRunner {
    private val googlePubSubClient = GooglePubSubClient(config.projectId, SERVICE_NAME)

    init {
        println("$SERVICE_NAME started running..")
    }

    override fun run(args: ApplicationArguments?) {
        val subscriptionId = "$ORDER_DELIVERY_SERVICE_SUBSCRIPTION_PREFIX-$ORDER_OUT_FOR_DELIVERY_TOPIC"
        val subscriptionName = ProjectSubscriptionName.format(config.projectId, subscriptionId)
        val topicName = ProjectTopicName.format(config.projectId, ORDER_OUT_FOR_DELIVERY_TOPIC)

        println("[$SERVICE_NAME] Creating subscription $subscriptionName for topic $topicName")
        googlePubSubClient.createPullSubscription(topicName, subscriptionName)

        val messageReceiver = { message: PubsubMessage, consumer: AckReplyConsumer ->
            initiateOrderDelivery(message.data.toStringUtf8())
            consumer.ack()
        }
        val subscriber = googlePubSubClient.createSubscriber(subscriptionName, messageReceiver)
        try {
            subscriber.startAsync().awaitRunning()
            println("[$SERVICE_NAME] Listening for messages on subscription: $subscriptionName")
        } catch (e: Exception) {
            println("[$SERVICE_NAME] Exception for $subscriptionName: ${e.message}")
            subscriber.stopAsync()
        }
    }

    private fun initiateOrderDelivery(
        orderDeliveryRequest: String
    ) {
        println("[$SERVICE_NAME] Received message on topic $ORDER_OUT_FOR_DELIVERY_TOPIC - $orderDeliveryRequest")

        val request = try {
            ObjectMapper().apply {
                configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true)
            }.readValue(orderDeliveryRequest, OrderDeliveryRequest::class.java)
        } catch (e: Exception) {
            throw e
        }

        orderRepository.save(
            Order(
                id = request.orderId,
                lastUpdatedDate = request.deliveryDate,
                status = OrderStatus.SHIPPED
            )
        )
        println("[$SERVICE_NAME] Order with orderId '${request.orderId}' is ${OrderStatus.SHIPPED}")
    }

    fun findById(orderId: Int, status: OrderStatus): Order? {
        return orderRepository.findById(orderId, status)
    }

}

data class OrderDeliveryRequest(
    @JsonProperty("orderId")
    val orderId: Int,
    @JsonProperty("deliveryAddress")
    val deliveryAddress: String,
    @JsonProperty("deliveryDate")
    val deliveryDate: String
)
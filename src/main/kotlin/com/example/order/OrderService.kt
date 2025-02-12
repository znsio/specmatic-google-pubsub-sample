package com.example.order

import com.google.cloud.pubsub.v1.AckReplyConsumer
import com.google.gson.Gson
import com.google.pubsub.v1.ProjectSubscriptionName
import com.google.pubsub.v1.ProjectTopicName
import com.google.pubsub.v1.PubsubMessage
import org.springframework.stereotype.Service
import java.math.BigDecimal

private const val ORDERERVICE_SUBSCRIPTION_PREFIX = "orderservice-subscription"
private const val ORDER_STATUS_PROCESSED = "PROCESSED"
private const val ORDER_STATUS_CANCELLED = "CANCELLED"
private const val NOTIFICATION_TYPE_ORDER_PLACED = "ORDER_PLACED"
private const val SERVICE_NAME = "Order Service"

@Service
class OrderService(private val config: Configuration) {

    private val placeOrderTopic = "place-order"
    private val processOrderTopic = "process-order"
    private val notificationTopic = "notification"
    private val googlePubSubClient = GooglePubSubClient(config.projectId, SERVICE_NAME)
    private val gson = Gson()


    fun run() {
        val subscriptionId = "$ORDERERVICE_SUBSCRIPTION_PREFIX-$placeOrderTopic"
        val subscriptionName = ProjectSubscriptionName.format(config.projectId, subscriptionId)
        val topicName = ProjectTopicName.format(config.projectId, placeOrderTopic)

        println("Creating subscription $subscriptionName for topic $topicName")
        googlePubSubClient.createPullSubscription(topicName, subscriptionName)

        val messageReceiver = { message: PubsubMessage, consumer: AckReplyConsumer ->
            processMessage(placeOrderTopic, message.data.toStringUtf8(), message.attributesMap)
            consumer.ack()
        }
        val subscriber = googlePubSubClient.createSubscriber(subscriptionName, messageReceiver)
        try {
            subscriber.startAsync().awaitRunning()
            System.out.printf("Listening for messages on subscription: %s:\n", subscriptionName)
        } catch (e: Exception) {
            println("Exception for $subscriptionName: ${e.message}")
            subscriber.stopAsync()
        }
    }

    private fun processMessage(topic:String, message:String, attributes:Map<String, String>) {
        val prettyPrintedMessage = googlePubSubClient.prettyPrintedMessage(message, attributes)
        println("$SERVICE_NAME received a message on topic $topic: $prettyPrintedMessage")

        sendMessageOnProcessOrderTopic(message)
        sendMessageOnNotificationTopic()
    }

    private fun sendMessageOnProcessOrderTopic(message: String) {
        val orderRequest = gson.fromJson(message, OrderRequest::class.java)
        val totalAmount = orderRequest.orderItems.sumOf { it.price * BigDecimal(it.quantity) }
        val taskMessage = """{"totalAmount": $totalAmount, "status": "$ORDER_STATUS_PROCESSED"}"""

        googlePubSubClient.publish(processOrderTopic, taskMessage)
    }

    private fun sendMessageOnNotificationTopic() {
        val taskMessage = """{"message": "Order processed successfully", "type": "$NOTIFICATION_TYPE_ORDER_PLACED"}"""
        googlePubSubClient.publish(notificationTopic, taskMessage)
    }
}


data class OrderRequest(
    val orderItems: List<OrderItem>,
    val status: String
)

data class OrderItem(
    val id: Int,
    val name: String,
    val quantity: Int,
    val price: BigDecimal
)


package com.example.product

import com.google.cloud.pubsub.v1.AckReplyConsumer
import com.google.cloud.pubsub.v1.Subscriber
import com.google.gson.Gson
import com.google.pubsub.v1.ProjectSubscriptionName
import com.google.pubsub.v1.ProjectTopicName
import com.google.pubsub.v1.PubsubMessage
import org.springframework.stereotype.Service
import java.math.BigDecimal

private const val ORDERERVICE_SUBSCRIPTION_PREFIX = "orderservice-subscription"
private const val projectId = "pub-sub-demo-414308"
private const val ORDER_STATUS_PROCESSED = "PROCESSED"
private const val SERVICE_NAME = "Order Service"

@Service
class OrderService {

    private val placeOrderTopic = "place-order"
    private val processOrderTopic = "process-order"
    private val googlePubSubClient = GooglePubSubClient(projectId, SERVICE_NAME)
    private val gson = Gson()


    fun run() {
        val subscriptionId = "$ORDERERVICE_SUBSCRIPTION_PREFIX-$placeOrderTopic"
        val subscriptionName = ProjectSubscriptionName.format(projectId, subscriptionId)
        val topicName = ProjectTopicName.format(projectId, placeOrderTopic)

        println("Creating subscription $subscriptionName for topic $topicName")
        googlePubSubClient.createPullSubscription(topicName, subscriptionName)

        val messageReceiver = { message: PubsubMessage, consumer: AckReplyConsumer ->
            processMessage(placeOrderTopic, message.data.toStringUtf8(), message.attributesMap)
            consumer.ack()
        }
        val subscriber = Subscriber.newBuilder(subscriptionName, messageReceiver).build()
        try {
            subscriber.startAsync().awaitRunning();
            System.out.printf("Listening for messages on subscription: %s:\n", subscriptionName);
        } catch (e: Exception) {
            println("Exception for $subscriptionName: ${e.message}")
            subscriber.stopAsync()
        }
    }

    private fun processMessage(topic:String, message:String, attributes:Map<String, String>) {
        val prettyPrintedMessage = googlePubSubClient.prettyPrintedMessage(message, attributes)
        println("$SERVICE_NAME received a message on topic $topic: $prettyPrintedMessage")

        val orderRequest = gson.fromJson(message, OrderRequest::class.java)
        val totalAmount = orderRequest.orderItems.sumOf { it.price * BigDecimal(it.quantity) }
        val taskMessage = """{"totalAmount": $totalAmount, "status": "$ORDER_STATUS_PROCESSED"}"""

        googlePubSubClient.publish(processOrderTopic, taskMessage, mapOf("SOURCE_ID" to SERVICE_NAME))
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


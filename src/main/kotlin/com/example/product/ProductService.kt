package com.example.product

import com.google.cloud.pubsub.v1.AckReplyConsumer
import com.google.cloud.pubsub.v1.Subscriber
import com.google.gson.Gson
import com.google.pubsub.v1.ProjectSubscriptionName
import com.google.pubsub.v1.ProjectTopicName
import com.google.pubsub.v1.PubsubMessage
import org.springframework.stereotype.Service
import java.math.BigDecimal

private const val PRODUCTSERVICE_SUBSCRIPTION_PREFIX = "productservice-subscription"
private const val projectId = "pub-sub-demo-414308"
private const val ORDER_STATUS_PROCESSED = "PROCESSED"

@Service
class ProductService {

    private val placeOrderTopic = "place-order"
    private val processOrderTopic = "process-order"
    private val googlePubSubClient = GooglePubSubClient(projectId, "Product Service")
    private val gson = Gson()


    fun run() {
        val subscriptionId = "$PRODUCTSERVICE_SUBSCRIPTION_PREFIX-$placeOrderTopic"
        val subscriptionName = ProjectSubscriptionName.format(projectId, subscriptionId)
        val topicName = ProjectTopicName.format(projectId, placeOrderTopic)

        println("Creating subscription $subscriptionName for topic $topicName")
        googlePubSubClient.createPullSubscription(topicName, subscriptionName)

        val messageReceiver = { message: PubsubMessage, consumer: AckReplyConsumer ->
            processMessage(placeOrderTopic, message.data.toStringUtf8())
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

    private fun processMessage(topic:String, message:String){
        println("Product Service has received message on topic $topic: $message")
        val orderRequest = gson.fromJson(message, OrderRequest::class.java)
        val totalAmount = orderRequest.orderItems.sumOf { it.price * BigDecimal(it.quantity) }
        val taskMessage = """{"totalAmount": $totalAmount, "status": "$ORDER_STATUS_PROCESSED"}"""
        googlePubSubClient.publish(processOrderTopic, taskMessage)
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


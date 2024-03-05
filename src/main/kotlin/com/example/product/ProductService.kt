package com.example.product

import com.google.cloud.pubsub.v1.AckReplyConsumer
import com.google.cloud.pubsub.v1.Subscriber
import com.google.gson.Gson
import com.google.pubsub.v1.ProjectSubscriptionName
import com.google.pubsub.v1.ProjectTopicName
import com.google.pubsub.v1.PubsubMessage
import org.springframework.stereotype.Service
import kotlin.random.Random

private const val PRODUCTSERVICE_SUBSCRIPTION_PREFIX = "productservice-subscription"
private const val projectId = "pub-sub-demo-414308"

@Service
class ProductService {

    private val productsTopic = "demo.products"
    private val tasksTopic = "demo.tasks"
    private val googlePubSubClient = GooglePubSubClient(projectId)
    private val gson = Gson()


    fun run() {
        val subscriptionId = "$PRODUCTSERVICE_SUBSCRIPTION_PREFIX-$productsTopic"
        val subscriptionName = ProjectSubscriptionName.format(projectId, subscriptionId)
        val topicName = ProjectTopicName.format(projectId, productsTopic)

        println("Creating subscription $subscriptionName for topic $topicName")
        googlePubSubClient.createPullSubscription(topicName, subscriptionName)

        val messageReceiver = { message: PubsubMessage, consumer: AckReplyConsumer ->
            processMessage(productsTopic, message.data.toStringUtf8())
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
        println("Product Service received message on topic $topic: $message")
        val product = gson.fromJson(message, Product::class.java)
        val id = Random.nextInt(1, 1001)
        val taskMessage = """{"id": $id, "name": "${product.name} Task"}"""
        googlePubSubClient.publish(tasksTopic, taskMessage)
    }
}

data class Product(
    val id: Int,
    val name: String,
    val inventory: Int
)
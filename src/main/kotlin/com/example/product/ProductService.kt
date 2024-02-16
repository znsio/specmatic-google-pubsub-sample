package com.example.product

import com.google.cloud.pubsub.v1.AckReplyConsumer
import com.google.cloud.pubsub.v1.Subscriber
import com.google.pubsub.v1.ProjectSubscriptionName
import com.google.pubsub.v1.ProjectTopicName
import com.google.pubsub.v1.PubsubMessage
import org.springframework.stereotype.Service

private const val PRODUCTSERVICE_SUBSCRIPTION_PREFIX = "productservice-subscription"
private const val projectId = "pub-sub-demo-414308"

@Service
class ProductService {

    private val productsTopic = "demo.products"
    private val tasksTopic = "demo.tasks"
    private val googlePubSubClient = GooglePubSubClient(projectId)

    fun run() {
        val subscriptionId = "$PRODUCTSERVICE_SUBSCRIPTION_PREFIX-$productsTopic"
        val subscriptionName = ProjectSubscriptionName.format(projectId, subscriptionId)
        val topicName = ProjectTopicName.format(projectId, productsTopic)

        println("Creating subscription $subscriptionName for topic $topicName")
        googlePubSubClient.createPullSubscription(topicName, subscriptionName)

        val messageReceiver = { message: PubsubMessage, consumer: AckReplyConsumer ->
            consumer.ack()
            processMessage(productsTopic, message.data.toStringUtf8())
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

        val taskMessage = """{"id": 10, "name": "Some Task"}"""
        googlePubSubClient.publish(tasksTopic, taskMessage)
    }
}
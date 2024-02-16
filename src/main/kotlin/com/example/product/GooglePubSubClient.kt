package com.example.product

import com.google.api.core.ApiFuture
import com.google.cloud.pubsub.v1.Publisher
import com.google.cloud.pubsub.v1.SubscriptionAdminClient
import com.google.protobuf.ByteString
import com.google.pubsub.v1.*
import java.util.concurrent.TimeUnit


class GooglePubSubClient(private val projectId:String) {

    fun createPullSubscription(topicName:String, subscriptionName:String) {
        SubscriptionAdminClient.create().use { subscriptionAdminClient ->
            val subscriptionExists = subscriptionAdminClient.listSubscriptions(ProjectName.format(projectId))
                .iterateAll()
                .any { it.name == subscriptionName }

            if (!subscriptionExists) {
                subscriptionAdminClient.createSubscription(
                    Subscription.newBuilder()
                        .setName(subscriptionName)
                        .setTopic(topicName)
                        .build()
                ).also { println("Subscription created: ${it.name}") }
            }
            else {
                println("Subscription already exists")
            }
        }
    }

    fun deleteSubscription(subscriptionId: String) {
        val subscriptionName = ProjectSubscriptionName.format(projectId, subscriptionId)

        SubscriptionAdminClient.create().use { subscriptionAdminClient ->
            try {
                subscriptionAdminClient.deleteSubscription(subscriptionName)
                println("Subscription $subscriptionId deleted.")
            } catch (e: Exception) {
                println("Error deleting subscription: ${e.message}")
            }
        }
    }

    fun publish(topicId: String, message: String) {
        val topicName = TopicName.of(projectId, topicId)
        var publisher: Publisher? = null
        try {
            println("Publishing message: $message")
            // Create a publisher instance with default settings bound to the topic
            publisher = Publisher.newBuilder(topicName).build()
            val data: ByteString = ByteString.copyFromUtf8(message)
            val pubsubMessage = PubsubMessage.newBuilder().setData(data).build()

            // Once published, returns a server-assigned message id (unique within the topic)
            val messageIdFuture: ApiFuture<String> = publisher.publish(pubsubMessage)
            val messageId = messageIdFuture.get()
            println("Published message ID: $messageId")
        } finally {
            if (publisher != null) {
                // When finished with the publisher, shutdown to free up resources.
                publisher.shutdown()
                publisher.awaitTermination(1, TimeUnit.MINUTES)
            }
        }
    }
}
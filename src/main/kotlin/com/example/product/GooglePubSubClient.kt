package com.example.product

import com.google.api.core.ApiFuture
import com.google.cloud.pubsub.v1.Publisher
import com.google.cloud.pubsub.v1.SubscriptionAdminClient
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.protobuf.ByteString
import com.google.pubsub.v1.*
import java.lang.StringBuilder
import java.util.concurrent.TimeUnit


class GooglePubSubClient(private val projectId:String, private val serviceName: String) {

    private val gson = GsonBuilder().setPrettyPrinting().create()

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

    fun publish(topicId: String, message: String, attributes:Map<String, String> = emptyMap()) {
        val topicName = TopicName.of(projectId, topicId)
        var publisher: Publisher? = null
        try {
            // Create a publisher instance with default settings bound to the topic
            publisher = Publisher.newBuilder(topicName).build()
            val data: ByteString = ByteString.copyFromUtf8(message)
            val pubsubMessage = PubsubMessage.newBuilder().setData(data).putAllAttributes(attributes).build()

            // Once published, returns a server-assigned message id (unique within the topic)
            val messageIdFuture: ApiFuture<String> = publisher.publish(pubsubMessage)
            val messageId = messageIdFuture.get()

            val prettyPrintedMessage = prettyPrintedMessage(message, attributes)
            println("$serviceName has published a message wih ID: $messageId on topic: $topicId: $prettyPrintedMessage")
            prettyPrintedMessage(message, attributes)
        } finally {
            if (publisher != null) {
                // When finished with the publisher, shutdown to free up resources.
                publisher.shutdown()
                publisher.awaitTermination(1, TimeUnit.MINUTES)
            }
        }
    }

    fun prettyPrintedMessage(payload:String, attributes:Map<String, String>): String {
        val payloadJsonObject = JsonParser.parseString(payload)
        val prettyPrintedPayload = gson.toJson(payloadJsonObject)

        return StringBuilder().apply {
            append(System.lineSeparator())
            append("Data: $prettyPrintedPayload")
            append(System.lineSeparator())
            append("Attributes: ${gson.toJson(attributes)}")
            append(System.lineSeparator())
        }.toString()
    }
}
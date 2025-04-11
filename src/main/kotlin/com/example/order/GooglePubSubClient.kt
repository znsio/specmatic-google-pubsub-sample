package com.example.order

import com.google.api.core.ApiFuture
import com.google.api.gax.core.CredentialsProvider
import com.google.api.gax.core.NoCredentialsProvider
import com.google.api.gax.grpc.GrpcTransportChannel
import com.google.api.gax.rpc.FixedTransportChannelProvider
import com.google.api.gax.rpc.TransportChannelProvider
import com.google.cloud.pubsub.v1.*
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.protobuf.ByteString
import com.google.pubsub.v1.*
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import java.lang.StringBuilder
import java.util.concurrent.TimeUnit

class GooglePubSubClient(private val projectId:String, private val serviceName: String) {

    private val emulatorHost: String? = System.getenv("PUBSUB_EMULATOR_HOST")
    private val emulatorChannel: ManagedChannel? =
        emulatorHost?.let{ ManagedChannelBuilder.forTarget(it).usePlaintext().build() }

    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun createPullSubscription(topicName:String, subscriptionName:String) {
        subscriptionAdminClient().use { subscriptionAdminClient ->
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
            publisher = publisher(topicName)
            val data: ByteString = ByteString.copyFromUtf8(message)
            val pubsubMessage = PubsubMessage.newBuilder().setData(data).putAllAttributes(attributes).build()

            // Once published, returns a server-assigned message id (unique within the topic)
            val messageIdFuture: ApiFuture<String> = publisher.publish(pubsubMessage)
            val messageId = messageIdFuture.get()

            val prettyPrintedMessage = prettyPrintedMessage(message, attributes)
            println("[$serviceName] has published a message wih ID: $messageId on topic: $topicId: $prettyPrintedMessage")
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

    fun createSubscriber(subscriptionName: String, messageReceiver: (PubsubMessage, AckReplyConsumer) -> Unit): Subscriber {
        return emulatorChannel?.let {
            val channelProvider: TransportChannelProvider =
                FixedTransportChannelProvider.create(GrpcTransportChannel.create(it))
            val credentialsProvider: CredentialsProvider = NoCredentialsProvider.create()
            Subscriber.newBuilder(subscriptionName, messageReceiver)
                .setChannelProvider(channelProvider)
                .setCredentialsProvider(credentialsProvider)
                .build()
        } ?: Subscriber.newBuilder(subscriptionName, messageReceiver)
            .build()
    }

    private fun subscriptionAdminClient(): SubscriptionAdminClient {
        return emulatorChannel?.let {
            val channelProvider: TransportChannelProvider =
                FixedTransportChannelProvider.create(GrpcTransportChannel.create(it))
            val credentialsProvider: CredentialsProvider = NoCredentialsProvider.create()
            SubscriptionAdminClient.create(
                SubscriptionAdminSettings.newBuilder()
                    .setTransportChannelProvider(channelProvider)
                    .setCredentialsProvider(credentialsProvider)
                    .build()
            )

        } ?: SubscriptionAdminClient.create()
    }

    private fun publisher(topicName: TopicName): Publisher {
        return emulatorChannel?.let {
            val channelProvider: TransportChannelProvider =
                FixedTransportChannelProvider.create(GrpcTransportChannel.create(it))
            val credentialsProvider: CredentialsProvider = NoCredentialsProvider.create()

            Publisher.newBuilder(topicName)
                .setChannelProvider(channelProvider)
                .setCredentialsProvider(credentialsProvider)
                .build()

        } ?: Publisher.newBuilder(topicName).build()
    }
}
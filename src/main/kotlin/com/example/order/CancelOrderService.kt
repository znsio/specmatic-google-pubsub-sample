package com.example.order

import com.google.cloud.pubsub.v1.AckReplyConsumer
import com.google.gson.Gson
import com.google.pubsub.v1.ProjectSubscriptionName
import com.google.pubsub.v1.ProjectTopicName
import com.google.pubsub.v1.PubsubMessage
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Service

private const val ORDER_SERVICE_SUBSCRIPTION_PREFIX = "orderservice-subscription"
private const val ORDER_STATUS = "COMPLETED"
private const val SERVICE_NAME = "CancelOrderService"

@Service
class CancelOrderService(private val config: Configuration): ApplicationRunner {
    private val cancelOrderTopic = "cancel-order"
    private val processCancellationTopic = "process-cancellation"
    private val googlePubSubClient = GooglePubSubClient(config.projectId, SERVICE_NAME)
    private val gson = Gson()

    override fun run(args: ApplicationArguments?) {
        val subscriptionId = "$ORDER_SERVICE_SUBSCRIPTION_PREFIX-$cancelOrderTopic"
        val subscriptionName = ProjectSubscriptionName.format(config.projectId, subscriptionId)
        val topicName = ProjectTopicName.format(config.projectId, cancelOrderTopic)

        println("[$SERVICE_NAME] Creating subscription $subscriptionName for topic $topicName")
        googlePubSubClient.createPullSubscription(topicName, subscriptionName)

        val messageReceiver = { message: PubsubMessage, consumer: AckReplyConsumer ->
            processMessage(cancelOrderTopic, message.data.toStringUtf8(), message.attributesMap)
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

    private fun processMessage(topic:String, message:String, attributes:Map<String, String>) {
        val prettyPrintedMessage = googlePubSubClient.prettyPrintedMessage(message, attributes)
        println("[$SERVICE_NAME] Received a message on topic $topic: $prettyPrintedMessage")

        sendMessageOnProcessCancellationTopic(message)
    }

    private fun sendMessageOnProcessCancellationTopic(message: String) {
        val cancellationRequest = gson.fromJson(message, OrderId::class.java)
        val taskMessage = """{"reference": 345, "status": "$ORDER_STATUS"}"""

        googlePubSubClient.publish(processCancellationTopic, taskMessage, mapOf("SOURCE_ID" to SERVICE_NAME))
        println("[$SERVICE_NAME] Published a message on topic $processCancellationTopic: $taskMessage")
    }
}

data class OrderId(
    val id: Int
)
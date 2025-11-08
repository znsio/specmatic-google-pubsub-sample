package com.example.order

import com.example.order.OrderStatus.CANCELLED
import com.example.order.OrderStatus.INITIATED
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

private const val SERVICE_NAME = "OrderService"
private const val NEW_ORDERS_TOPIC = "new-orders"
private const val WIP_ORDERS_TOPIC = "wip-orders"
private const val TO_BE_CANCELLED_ORDERS_TOPIC = "to-be-cancelled-orders"
private const val CANCELLED_ORDERS_TOPIC = "cancelled-orders"
private const val ORDER_SERVICE_SUBSCRIPTION_PREFIX = "orderservice-subscription"

@Service
class OrderService(
    private val config: Configuration,
    private val orderRepository: OrderRepository
): ApplicationRunner {

    private val googlePubSubClient = GooglePubSubClient(config.projectId, SERVICE_NAME)

    init {
        println("$SERVICE_NAME started running..")
    }

    override fun run(args: ApplicationArguments?) {
        Thread {
            subscribeToNewOrdersTopic()
        }.start()
        Thread {
            subscribeToCancelledOrdersTopic()
        }.start()
    }

    private fun subscribeToNewOrdersTopic() {
        val subscriptionId = "$ORDER_SERVICE_SUBSCRIPTION_PREFIX-$NEW_ORDERS_TOPIC"
        val subscriptionName = ProjectSubscriptionName.format(config.projectId, subscriptionId)
        val topicName = ProjectTopicName.format(config.projectId, NEW_ORDERS_TOPIC)

        println("[$SERVICE_NAME] Creating subscription $subscriptionName for topic $topicName")
        googlePubSubClient.createPullSubscription(topicName, subscriptionName)

        val messageReceiver = { message: PubsubMessage, consumer: AckReplyConsumer ->
            val placeOrderRequest = message.data.toStringUtf8()
            println("[$SERVICE_NAME] Received message on topic $TO_BE_CANCELLED_ORDERS_TOPIC - $placeOrderRequest")
            val placeOrderRequestJson = try {
                ObjectMapper().apply {
                    configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true)
                }.readValue(placeOrderRequest, PlaceOrderRequest::class.java)
            } catch (e: Exception) {
                throw e
            }
            processPlaceOrderMessage(placeOrderRequestJson)
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

    private fun subscribeToCancelledOrdersTopic() {
        val subscriptionId = "$ORDER_SERVICE_SUBSCRIPTION_PREFIX-$TO_BE_CANCELLED_ORDERS_TOPIC"
        val subscriptionName = ProjectSubscriptionName.format(config.projectId, subscriptionId)
        val topicName = ProjectTopicName.format(config.projectId, TO_BE_CANCELLED_ORDERS_TOPIC)

        println("[$SERVICE_NAME] Creating subscription $subscriptionName for topic $topicName")
        googlePubSubClient.createPullSubscription(topicName, subscriptionName)

        val messageReceiver = { message: PubsubMessage, consumer: AckReplyConsumer ->
            val cancellationRequest = message.data.toStringUtf8()
            println("[$SERVICE_NAME] Received message on topic $TO_BE_CANCELLED_ORDERS_TOPIC - $cancellationRequest")

            val orderIdObject = try {
                ObjectMapper().apply {
                    configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true)
                }.readValue(cancellationRequest, OrderId::class.java)
            } catch (e: Exception) {
                throw e
            }
            processCancellation(orderIdObject)
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

    private fun processPlaceOrderMessage(placeOrderRequest: PlaceOrderRequest) {
        val order = Order(
            id = placeOrderRequest.id,
            orderItems = placeOrderRequest.orderItems,
            status = INITIATED
        )
        sendMessageOnProcessOrderTopic(order)
        orderRepository.save(order)
    }

    private fun processCancellation(id: OrderId) {
        val order = Order(
            id = id.id,
            status = CANCELLED
        )
        sendMessageOnProcessCancellationTopic(order)
        orderRepository.save(order)
    }

    private fun sendMessageOnProcessCancellationTopic(order: Order) {
        val cancellationMessage = """{"reference": ${order.id}, "status": "${order.status}"}"""

        println("[$SERVICE_NAME] Publishing a message on $CANCELLED_ORDERS_TOPIC topic: $cancellationMessage")
        googlePubSubClient.publish(CANCELLED_ORDERS_TOPIC, cancellationMessage)
    }

    private fun sendMessageOnProcessOrderTopic(order: Order) {
        val taskMessage =
            """{"id": ${order.id}, "totalAmount": ${order.totalAmount()}, "status": "${order.status}"}"""

        println("[$SERVICE_NAME] Publishing a message on $WIP_ORDERS_TOPIC topic: $taskMessage")
        googlePubSubClient.publish(WIP_ORDERS_TOPIC, taskMessage)
    }

}

data class PlaceOrderRequest(
    @JsonProperty("id")
    val id: Int,
    @JsonProperty("orderItems")
    val orderItems: List<OrderItem>
)
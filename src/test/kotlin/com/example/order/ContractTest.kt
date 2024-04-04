package com.example.order

import com.example.order.utils.PubSubEmulator
import `in`.specmatic.googlepubsub.mock.GooglePubSubMock
import `in`.specmatic.googlepubsub.mock.SpecmaticGooglePubSubTestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext

class ContractTest : SpecmaticGooglePubSubTestBase() {

    companion object {

        private const val PROJECT_ID = "pub-sub-demo-414308"
        private const val PLACE_ORDER_TOPIC = "place-order"
        private const val PROCESS_ORDER_TOPIC = "process-order"
        private const val NOTIFICATION_TOPIC = "notification"

        private lateinit var context: ConfigurableApplicationContext

        private val pubSubEmulator = PubSubEmulator(
            projectId = PROJECT_ID,
            topics = listOf(PLACE_ORDER_TOPIC, PROCESS_ORDER_TOPIC, NOTIFICATION_TOPIC),
        )

        @JvmStatic
        @BeforeAll
        fun setUp() {
            pubSubEmulator.start()
            googlePubSubMock = GooglePubSubMock.connectWithBroker(PROJECT_ID)
            context = runApplication<ProductServiceApplication>()
            context.getBean(ProductServiceApplication::class.java).run()
        }

        @JvmStatic
        @AfterAll
        fun tearDown() {
            context.stop()
            val result = googlePubSubMock.stop()
            assertThat(result.success).withFailMessage(result.errors.joinToString()).isTrue
            pubSubEmulator.stop()
        }
    }
}

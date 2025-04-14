package com.example.order

import com.example.order.utils.PubSubEmulator
import io.specmatic.googlepubsub.mock.GooglePubSubMock
import io.specmatic.googlepubsub.mock.SpecmaticGooglePubSubTestBase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext

class ContractTest : SpecmaticGooglePubSubTestBase() {

    companion object {
        private const val PROJECT_ID = "pub-sub-demo-414308"
        private const val SHUTDOWN_TIMEOUT_FOR_MOCK_IN_MS = 1000
        private lateinit var context: ConfigurableApplicationContext

        private val pubSubEmulator = PubSubEmulator(projectId = PROJECT_ID)

        @JvmStatic
        @BeforeAll
        fun setUp() {
            pubSubEmulator.start()
            googlePubSubMock = GooglePubSubMock.connectWithBroker(PROJECT_ID)
            context = runApplication<ProductServiceApplication>()
        }

        @JvmStatic
        @AfterAll
        fun tearDown() {
            context.stop()
            googlePubSubMock.stop(SHUTDOWN_TIMEOUT_FOR_MOCK_IN_MS)
            pubSubEmulator.stop()
        }
    }
}

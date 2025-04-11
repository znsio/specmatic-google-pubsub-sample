package com.example.order

import com.example.order.utils.PubSubEmulator
import io.specmatic.googlepubsub.mock.GooglePubSubMock
import io.specmatic.googlepubsub.mock.SpecmaticGooglePubSubTestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext

class ContractTest : SpecmaticGooglePubSubTestBase() {

    companion object {
        private const val PROJECT_ID = "pub-sub-demo-414308"
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
            val result = googlePubSubMock.stop()
            assertThat(result.success).withFailMessage(result.errors.joinToString()).isTrue
            pubSubEmulator.stop()
        }
    }
}

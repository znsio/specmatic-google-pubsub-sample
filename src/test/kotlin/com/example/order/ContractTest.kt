package com.example.order

import com.example.order.utils.PubSubEmulator
import io.specmatic.async.core.ExecutionMode
import io.specmatic.async.core.constants.GOOGLE_PUB_SUB_EMULATOR_HOST
import io.specmatic.async.core.constants.GOOGLE_PUB_SUB_PROJECT_ID
import io.specmatic.googlepubsub.client.PubSubClient
import io.specmatic.googlepubsub.test.SpecmaticGooglePubSubContractTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext

class ContractTest : SpecmaticGooglePubSubContractTest {

    companion object {
        private const val PROJECT_ID = "pub-sub-demo-414308"
        private lateinit var context: ConfigurableApplicationContext

        private val pubSubEmulator = PubSubEmulator(projectId = PROJECT_ID)

        @JvmStatic
        @BeforeAll
        fun setUp() {
            pubSubEmulator.start()

            System.setProperty(GOOGLE_PUB_SUB_PROJECT_ID, PROJECT_ID)
            System.setProperty(GOOGLE_PUB_SUB_EMULATOR_HOST, "localhost:8085")

            PubSubClient(PROJECT_ID, ExecutionMode.TEST).apply {
                listOf("place-order", "process-order", "notification").forEach {
                    createTopic(it)
                }
                createSubscriptions( listOf("place-order"))
                Thread.sleep(1000)
            }

            context = runApplication<ProductServiceApplication>()
            context.getBean(ProductServiceApplication::class.java).run()
        }

        @JvmStatic
        @AfterAll
        fun tearDown() {
            context.stop()
            pubSubEmulator.stop()
        }
    }
}

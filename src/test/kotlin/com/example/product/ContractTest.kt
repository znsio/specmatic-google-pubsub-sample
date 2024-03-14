package com.example.product

import `in`.specmatic.googlepubsub.mock.GooglePubSubMock
import `in`.specmatic.googlepubsub.mock.SpecmaticGooglePubSubTestBase
import `in`.specmatic.googlepubsub.mock.model.Expectation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext


class ContractTest : SpecmaticGooglePubSubTestBase() {

    companion object {

        private const val projectId = "pub-sub-demo-414308"
        private lateinit var context: ConfigurableApplicationContext
        private const val productsTopic = "demo.products"
        private const val tasksTopic = "demo.tasks"

        @JvmStatic
        @BeforeAll
        fun setUp() {
            googlePubSubMock = GooglePubSubMock.connectWithBroker(projectId, 15000)

            googlePubSubMock.setExpectations(
                listOf(
                    Expectation(productsTopic, 1),
                    Expectation(tasksTopic, 1)
                )
            )

            context = runApplication<ProductServiceApplication>()
            context.getBean(ProductServiceApplication::class.java).run()
        }

        @JvmStatic
        @AfterAll
        fun tearDown() {
            val result = googlePubSubMock.verifyExpectations()
            context.stop()
            googlePubSubMock.stop()
            assertThat(result.success).withFailMessage(result.errors.joinToString()).isTrue
        }
    }
}

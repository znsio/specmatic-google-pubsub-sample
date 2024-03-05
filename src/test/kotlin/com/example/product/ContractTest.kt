package com.example.product

import `in`.specmatic.googlepubsub.mock.GooglePubSubMock
import `in`.specmatic.googlepubsub.mock.model.Expectation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext


class ContractTest {

    @Test
    fun contractTests() {
        googlePubSubMock.executeTests()
        googlePubSubMock.setExpectations(
            listOf(
                Expectation(productsTopic, 2),
                Expectation(tasksTopic, 2)
            )
        )
        googlePubSubMock.awaitMessages(4)
        val result = googlePubSubMock.verifyExpectations()
        if (!result.success) {
            println(result.errors)
        }
        assertThat(result.success).withFailMessage(result.errors.joinToString()).isTrue
    }

    companion object {

        private const val projectId = "pub-sub-demo-414308"
        private lateinit var googlePubSubMock: GooglePubSubMock
        private lateinit var context: ConfigurableApplicationContext
        private const val productsTopic = "demo.products"
        private const val tasksTopic = "demo.tasks"

        @JvmStatic
        @BeforeAll
        fun setUp() {
            googlePubSubMock = GooglePubSubMock.connectWithBroker(projectId)
            googlePubSubMock.start()

            context = runApplication<ProductServiceApplication>()
            context.getBean(ProductServiceApplication::class.java).run()
        }

        @JvmStatic
        @AfterAll
        fun tearDown(){
            context.stop()
            googlePubSubMock.stop()
        }
    }
}

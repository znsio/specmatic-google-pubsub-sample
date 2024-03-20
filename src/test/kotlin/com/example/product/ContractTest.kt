package com.example.product

import `in`.specmatic.googlepubsub.mock.GooglePubSubMock
import `in`.specmatic.googlepubsub.mock.SpecmaticGooglePubSubTestBase
import `in`.specmatic.googlepubsub.mock.model.Expectation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext


class ContractTest : SpecmaticGooglePubSubTestBase(15000) {

    companion object {

        private const val projectId = "pub-sub-demo-414308"
        private lateinit var context: ConfigurableApplicationContext

        @JvmStatic
        @BeforeAll
        fun setUp() {
            googlePubSubMock = GooglePubSubMock.connectWithBroker(projectId)
            context = runApplication<ProductServiceApplication>()
            context.getBean(ProductServiceApplication::class.java).run()
        }

        @JvmStatic
        @AfterAll
        fun tearDown() {
            context.stop()
            val result = googlePubSubMock.stop()
            assertThat(result.success).withFailMessage(result.errors.joinToString()).isTrue
        }
    }
}

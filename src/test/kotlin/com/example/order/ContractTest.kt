package com.example.order

import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.PortBinding
import com.github.dockerjava.api.model.Ports
import `in`.specmatic.googlepubsub.mock.GooglePubSubMock
import `in`.specmatic.googlepubsub.mock.SpecmaticGooglePubSubTestBase
import io.grpc.netty.shaded.io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue.WaitStrategy
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait

class ContractTest : SpecmaticGooglePubSubTestBase() {

    companion object {

        private const val projectId = "pub-sub-demo-414308"
        private lateinit var context: ConfigurableApplicationContext

        private val container : GenericContainer<*>  = GenericContainer(
            "gcr.io/google.com/cloudsdktool/google-cloud-cli:emulators"
        ).withExposedPorts(8085)
            .withCreateContainerCmdModifier {
                it.withHostConfig(
                    HostConfig().withPortBindings(
                        PortBinding(Ports.Binding.bindPort(8085), ExposedPort(8085))
                    )
                )
            }
            .withEnv("PUBSUB_EMULATOR_HOST", "localhost:8085")
            .withCommand("/bin/bash", "-c", "gcloud beta emulators pubsub start --project=$projectId --host-port=0.0.0.0:8085")

        @JvmStatic
        @BeforeAll
        fun setUp() {
            container.start()
            container.execInContainer(
                "/bin/bash",
                "-c",
                """
                    curl -X PUT http://localhost:8085/v1/projects/${projectId}/topics/place-order
                    curl -X PUT http://localhost:8085/v1/projects/${projectId}/topics/process-order
                    curl -X PUT http://localhost:8085/v1/projects/${projectId}/topics/notification
                    echo "topics created"
                """.trimIndent()
            )
            container.waitingFor(Wait.forLogMessage("topics created", 1))
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
            container.stop()
        }
    }
}

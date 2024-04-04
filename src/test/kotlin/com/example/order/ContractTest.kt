package com.example.order

import com.github.dockerjava.api.command.CreateContainerCmd
import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.PortBinding
import com.github.dockerjava.api.model.Ports
import `in`.specmatic.googlepubsub.mock.GooglePubSubMock
import `in`.specmatic.googlepubsub.mock.SpecmaticGooglePubSubTestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.util.function.Consumer

class ContractTest : SpecmaticGooglePubSubTestBase() {

    companion object {

        private const val PROJECT_ID = "pub-sub-demo-414308"
        private lateinit var context: ConfigurableApplicationContext

        private const val EMULATOR_PORT = 8085
        private const val TOPIC_CREATION_LOG = "Topics creation completed. Emulator is ready.."
        private val emulator = createEmulatorContainer()

        @JvmStatic
        @BeforeAll
        fun setUp() {
            emulator.start()
            emulator.execInContainer("/bin/bash", "-c", createTopicsCommand())
            emulator.waitingFor(Wait.forLogMessage(TOPIC_CREATION_LOG, 1))

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
            emulator.stop()
        }

        private fun createEmulatorContainer() : GenericContainer<*> {
            val bindContainerPortToHost = Consumer<CreateContainerCmd> {
                it.withHostConfig(
                    HostConfig().withPortBindings(
                        PortBinding(Ports.Binding.bindPort(EMULATOR_PORT), ExposedPort(EMULATOR_PORT))
                    )
                )
            }

            return GenericContainer(
                "gcr.io/google.com/cloudsdktool/google-cloud-cli:emulators"
            ).withExposedPorts(EMULATOR_PORT)
                .withCreateContainerCmdModifier(bindContainerPortToHost)
                .withEnv("PUBSUB_EMULATOR_HOST", "localhost:$EMULATOR_PORT")
                .withCommand("gcloud beta emulators pubsub start --project=$PROJECT_ID --host-port=0.0.0.0:$EMULATOR_PORT")
        }

        private fun createTopicsCommand() : String {
            val createTopicUrl = "http://localhost:$EMULATOR_PORT/v1/projects/${PROJECT_ID}/topics"
            val script = StringBuilder()
            val topics = listOf("place-order", "process-order", "notification")

            for(topic in topics) {
                script.append("curl -X PUT $createTopicUrl/$topic\n")
            }
            script.append("echo $TOPIC_CREATION_LOG\n")

            return script.toString()
        }
    }
}

package com.example.order.utils

import com.github.dockerjava.api.command.CreateContainerCmd
import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.PortBinding
import com.github.dockerjava.api.model.Ports
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.util.function.Consumer

class PubSubEmulator(
    private val projectId: String,
) {
    private val port = 8085
    private val host = "localhost:$port"
    private val startupMessage = "PubSub Emulator is ready."

    private val emulator = createEmulatorContainer()

    fun start() {
        emulator.start()
        logStartupMessage()
        emulator.waitingFor(Wait.forLogMessage(startupMessage, 1))
    }

    fun stop() = emulator.stop()

    private fun createEmulatorContainer(): GenericContainer<*> {
        val bindContainerPortToHost = Consumer<CreateContainerCmd> {
            it.withHostConfig(
                HostConfig().withPortBindings(
                    PortBinding(Ports.Binding.bindPort(port), ExposedPort(port))
                )
            )
        }

        return GenericContainer(
            "gcr.io/google.com/cloudsdktool/google-cloud-cli:emulators"
        ).withExposedPorts(port)
            .withCreateContainerCmdModifier(bindContainerPortToHost)
            .withEnv("PUBSUB_EMULATOR_HOST", host)
            .withCommand("gcloud beta emulators pubsub start --project=$projectId --host-port=0.0.0.0:$port")
    }

    private fun logStartupMessage() = emulator.execInContainer(
        "/bin/bash",
        "-c",
        "echo $startupMessage"
    )
}
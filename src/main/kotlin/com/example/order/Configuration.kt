package com.example.order

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
class Configuration {
    @Value("\${google.pubsub.projectId}")
    lateinit var projectId: String
}
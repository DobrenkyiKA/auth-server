package com.kdob.piq.authserver.event

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class UserEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, UserCreatedEvent>,
    @Value("\${app.kafka.topic.user.created") private val userCreatedTopic: String
) {
    private val logger = LoggerFactory.getLogger(UserEventPublisher::class.java)

    fun publishUserCreated(event: UserCreatedEvent) {
        logger.info("Publishing UserCreatedEvent for userId=[{}]", event.authId)
        kafkaTemplate.send(userCreatedTopic, event.authId.toString(), event)
            .whenComplete { result, ex ->
                if (ex != null) {
                    logger.error("Failed to publish UserCreatedEvent for userId=[{}]", event.authId, ex)
                } else {
                    logger.info(
                        "UserCreatedEvent published for userId=[{}], offset=[{}]",
                        event.authId,
                        result.recordMetadata.offset()
                    )
                }
            }
    }
}
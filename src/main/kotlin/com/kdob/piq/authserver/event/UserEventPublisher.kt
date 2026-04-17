package com.kdob.piq.authserver.event

import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class UserEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, UserCreatedEvent>
) {
    private val logger = LoggerFactory.getLogger(UserEventPublisher::class.java)

    fun publishUserCreated(event: UserCreatedEvent) {
        logger.info("Publishing UserCreatedEvent for userId=[{}]", event.userId)
        kafkaTemplate.send("user-created", event.userId.toString(), event)
            .whenComplete { result, ex ->
                if (ex != null) {
                    logger.error("Failed to publish UserCreatedEvent for userId=[{}]", event.userId, ex)
                } else {
                    logger.info(
                        "UserCreatedEvent published for userId=[{}], offset=[{}]",
                        event.userId,
                        result.recordMetadata.offset()
                    )
                }
            }
    }
}
package com.frauddetection.notification.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic verifiedTransactionsTopic() {
        return TopicBuilder.name("transactions.verified")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic fraudTransactionsTopic() {
        return TopicBuilder.name("transactions.fraud")
                .partitions(3)
                .replicas(1)
                .build();
    }
}

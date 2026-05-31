package com.finance.transactionservice.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class KafkaHeartbeatScheduler {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    // Send every 12 hours (43200000 ms) to keep Aiven Kafka active
    @Scheduled(fixedRate = 43200000)
    public void sendHeartbeat() {
        // We use a separate 'heartbeat-events' topic so we don't crash the Notification Service
        // which expects valid TransactionEvent JSON objects on 'transaction-events'
        kafkaTemplate.send("heartbeat-events", "heartbeat");
        System.out.println("Kafka heartbeat sent to keep Aiven awake.");
    }
}

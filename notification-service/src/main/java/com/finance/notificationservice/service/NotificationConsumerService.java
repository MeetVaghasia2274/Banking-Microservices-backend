package com.finance.notificationservice.service;

import com.finance.notificationservice.model.AccountDto;
import com.finance.notificationservice.model.Notification;
import com.finance.notificationservice.model.TransactionEvent;
import com.finance.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class NotificationConsumerService {

    private final NotificationRepository notificationRepository;
    private final RestTemplate restTemplate;

    @Value("${app.account-service.url}")
    private String accountServiceUrl;

    @KafkaListener(topics = "transaction-events", groupId = "notification-group")
    public void consumeTransactionEvent(TransactionEvent event) {
        System.out.println("Received Kafka Event: " + event);

        try {
            // 1. Fetch account details for sender (fromAccount)
            AccountDto senderAccount = fetchAccountDetailsByNumber(event.getFromAccount());
            if (senderAccount != null) {
                String senderMessage = String.format("Alert: Your account %s was debited by $%s to account %s. Ref: %s",
                        event.getFromAccount(), event.getAmount(), event.getToAccount(), event.getTransactionId());
                
                Notification senderNotification = Notification.builder()
                        .userId(senderAccount.getUserId())
                        .message(senderMessage)
                        .readStatus(false)
                        .build();
                
                notificationRepository.save(senderNotification);
                
                // Simulate SMS/Email Logging
                System.out.println("[SMS/EMAIL SENT to User " + senderAccount.getUserId() + "]: " + senderMessage);
            }

            // 2. Fetch account details for receiver (toAccount)
            AccountDto receiverAccount = fetchAccountDetailsByNumber(event.getToAccount());
            if (receiverAccount != null) {
                String receiverMessage = String.format("Alert: Your account %s was credited by $%s from account %s. Ref: %s",
                        event.getToAccount(), event.getAmount(), event.getFromAccount(), event.getTransactionId());
                
                Notification receiverNotification = Notification.builder()
                        .userId(receiverAccount.getUserId())
                        .message(receiverMessage)
                        .readStatus(false)
                        .build();
                
                notificationRepository.save(receiverNotification);
                
                // Simulate SMS/Email Logging
                System.out.println("[SMS/EMAIL SENT to User " + receiverAccount.getUserId() + "]: " + receiverMessage);
            }

        } catch (Exception e) {
            System.err.println("Error processing transaction event in Notification Service: " + e.getMessage());
        }
    }

    public java.util.List<Notification> getNotificationsForUser(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    private AccountDto fetchAccountDetailsByNumber(String accountNumber) {
        try {
            String baseUrl = accountServiceUrl;
            if (!baseUrl.contains("/api/accounts")) {
                baseUrl = baseUrl.endsWith("/") ? baseUrl + "api/accounts" : baseUrl + "/api/accounts";
            }
            String url = baseUrl + "/details/number/" + accountNumber;
            return restTemplate.getForObject(url, AccountDto.class);
        } catch (Exception e) {
            System.err.println("Could not fetch account details for account number: " + accountNumber + ". Error: " + e.getMessage());
            return null;
        }
    }
}

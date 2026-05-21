package com.finance.transactionservice.service;

import com.finance.transactionservice.model.AccountDto;
import com.finance.transactionservice.model.Transaction;
import com.finance.transactionservice.model.TransactionEvent;
import com.finance.transactionservice.model.TransactionType;
import com.finance.transactionservice.model.TransferRequest;
import com.finance.transactionservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final RestTemplate restTemplate;
    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    @Value("${app.account-service.url}")
    private String accountServiceUrl;

    private static final String TOPIC = "transaction-events";

    @Transactional
    public Transaction transferMoney(TransferRequest request) {
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be greater than zero");
        }

        // 1. Fetch sender and receiver account details from Account Service
        AccountDto fromAccount = fetchAccountDetails(request.getFromAccountId());
        AccountDto toAccount = fetchAccountDetails(request.getToAccountId());

        // 2. Validate sender balance
        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("Insufficient balance in source account");
        }

        // 3. Update balances
        BigDecimal newFromBalance = fromAccount.getBalance().subtract(request.getAmount());
        BigDecimal newToBalance = toAccount.getBalance().add(request.getAmount());

        updateAccountBalance(fromAccount.getId(), newFromBalance);
        updateAccountBalance(toAccount.getId(), newToBalance);

        // 4. Save transaction in DB
        Transaction transaction = Transaction.builder()
                .fromAccountId(request.getFromAccountId())
                .toAccountId(request.getToAccountId())
                .amount(request.getAmount())
                .type(TransactionType.TRANSFER)
                .description(request.getDescription())
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);

        // 5. Publish Event to Kafka
        TransactionEvent event = TransactionEvent.builder()
                .transactionId(UUID.randomUUID().toString())
                .fromAccount(fromAccount.getAccountNumber())
                .toAccount(toAccount.getAccountNumber())
                .amount(request.getAmount())
                .type("TRANSFER")
                .timestamp(LocalDateTime.now())
                .build();

        try {
            kafkaTemplate.send(TOPIC, event);
            System.out.println("Published Kafka transaction-event: " + event);
        } catch (Exception e) {
            System.err.println("Failed to publish Kafka event: " + e.getMessage());
        }

        return savedTransaction;
    }

    public List<Transaction> getTransactionHistory(Long accountId) {
        return transactionRepository.findByAccountId(accountId);
    }

    private AccountDto fetchAccountDetails(Long accountId) {
        try {
            String baseUrl = accountServiceUrl;
            if (!baseUrl.contains("/api/accounts")) {
                baseUrl = baseUrl.endsWith("/") ? baseUrl + "api/accounts" : baseUrl + "/api/accounts";
            }
            String url = baseUrl + "/details/" + accountId;
            return restTemplate.getForObject(url, AccountDto.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch account details for accountId: " + accountId + ". Error: " + e.getMessage());
        }
    }

    private void updateAccountBalance(Long accountId, BigDecimal newBalance) {
        try {
            String baseUrl = accountServiceUrl;
            if (!baseUrl.contains("/api/accounts")) {
                baseUrl = baseUrl.endsWith("/") ? baseUrl + "api/accounts" : baseUrl + "/api/accounts";
            }
            String url = baseUrl + "/balance/" + accountId;
            Map<String, BigDecimal> requestBody = new HashMap<>();
            requestBody.put("balance", newBalance);
            restTemplate.put(url, requestBody);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update account balance for accountId: " + accountId + ". Error: " + e.getMessage());
        }
    }
}

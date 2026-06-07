package com.finance.transactionservice.service;

import com.finance.transactionservice.model.AccountDto;
import com.finance.transactionservice.model.TransferRequest;
import com.finance.transactionservice.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void transferMoney_InsufficientBalance_ThrowsException() {
        // Arrange
        ReflectionTestUtils.setField(transactionService, "accountServiceUrl", "http://account-service/api/accounts");

        TransferRequest request = new TransferRequest();
        request.setFromAccountId(1L);
        request.setToAccountId(2L);
        request.setAmount(new BigDecimal("500.00")); // Trying to transfer 500

        AccountDto fromAccount = new AccountDto();
        fromAccount.setId(1L);
        fromAccount.setAccountNumber("ACC1");
        fromAccount.setBalance(new BigDecimal("100.00")); // But only has 100

        AccountDto toAccount = new AccountDto();
        toAccount.setId(2L);
        toAccount.setAccountNumber("ACC2");
        toAccount.setBalance(new BigDecimal("1000.00"));

        when(restTemplate.getForObject(contains("/details/1"), eq(AccountDto.class))).thenReturn(fromAccount);
        when(restTemplate.getForObject(contains("/details/2"), eq(AccountDto.class))).thenReturn(toAccount);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> transactionService.transferMoney(request));
        assertEquals("Insufficient balance in source account", exception.getMessage());
        
        // Verify balance was never updated and transaction never saved
        verify(restTemplate, never()).put(anyString(), any());
        verify(transactionRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(anyString(), any());
    }
}

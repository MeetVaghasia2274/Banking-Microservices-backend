package com.finance.accountservice.service;

import com.finance.accountservice.model.Account;
import com.finance.accountservice.model.AccountType;
import com.finance.accountservice.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AccountService accountService;

    @Test
    void getBalance_FromRedisCache_DbNotHit() {
        // Arrange
        Long accountId = 1L;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("balance:1")).thenReturn("500.00");

        // Act
        BigDecimal balance = accountService.getBalance(accountId);

        // Assert
        assertEquals(new BigDecimal("500.00"), balance);
        verify(accountRepository, never()).findById(anyLong());
    }

    @Test
    void getBalance_CacheEmpty_HitsDbAndStoresInRedis() {
        // Arrange
        Long accountId = 1L;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("balance:1")).thenReturn(null);

        Account account = Account.builder()
                .id(accountId)
                .balance(new BigDecimal("1000.00"))
                .build();

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        // Act
        BigDecimal balance = accountService.getBalance(accountId);

        // Assert
        assertEquals(new BigDecimal("1000.00"), balance);
        verify(accountRepository, times(1)).findById(accountId);
        verify(valueOperations, times(1)).set("balance:1", "1000.00", 300, TimeUnit.SECONDS);
    }

    @Test
    void createAccount_Success() {
        // Arrange
        Long userId = 1L;
        AccountType type = AccountType.SAVINGS;

        Account savedAccount = Account.builder()
                .id(100L)
                .userId(userId)
                .accountNumber("ACC12345678")
                .accountType(type)
                .balance(BigDecimal.ZERO)
                .build();

        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);

        // Act
        Account account = accountService.createAccount(userId, type);

        // Assert
        assertNotNull(account);
        assertEquals(100L, account.getId());
        assertEquals("ACC12345678", account.getAccountNumber());
        assertEquals(BigDecimal.ZERO, account.getBalance());
        verify(accountRepository, times(1)).save(any(Account.class));
    }
}

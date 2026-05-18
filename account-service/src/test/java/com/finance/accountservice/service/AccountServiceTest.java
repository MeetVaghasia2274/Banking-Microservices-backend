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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
    void shouldReturnCachedBalanceFromRedis() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("balance:1")).thenReturn("5000.00");

        BigDecimal balance = accountService.getBalance(1L);

        assertEquals(new BigDecimal("5000.00"), balance);
        verify(accountRepository, never()).findById(any()); // DB was NOT hit
    }

    @Test
    void shouldFetchBalanceFromDbAndCacheIfNotFoundInRedis() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("balance:1")).thenReturn(null);

        Account account = Account.builder()
                .id(1L)
                .userId(100L)
                .accountNumber("ACC123")
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("5000.00"))
                .build();

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        BigDecimal balance = accountService.getBalance(1L);

        assertEquals(new BigDecimal("5000.00"), balance);
        verify(accountRepository, times(1)).findById(1L);
        verify(valueOperations, times(1)).set(eq("balance:1"), eq("5000.00"), eq(300L), any());
    }
}

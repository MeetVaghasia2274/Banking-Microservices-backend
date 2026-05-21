package com.finance.accountservice.service;

import com.finance.accountservice.model.Account;
import com.finance.accountservice.model.AccountType;
import com.finance.accountservice.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final RedisTemplate<String, String> redisTemplate;

    public Account createAccount(Long userId, AccountType accountType) {
        String accountNumber = "ACC" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        Account account = Account.builder()
                .userId(userId)
                .accountNumber(accountNumber)
                .accountType(accountType)
                .balance(BigDecimal.ZERO)
                .build();
                
        return accountRepository.save(account);
    }

    public List<Account> getAccountsByUserId(Long userId) {
        return accountRepository.findByUserId(userId);
    }

    public Account getAccountById(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
    }

    public Account getAccountByAccountNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found for number: " + accountNumber));
    }

    public BigDecimal getBalance(Long accountId) {
        String cacheKey = "balance:" + accountId;
        try {
            String cachedBalance = redisTemplate.opsForValue().get(cacheKey);
            if (cachedBalance != null) {
                return new BigDecimal(cachedBalance);
            }
        } catch (Exception e) {
            System.err.println("[WARN] Redis is unavailable for getBalance: " + e.getMessage());
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        BigDecimal balance = account.getBalance();
        
        try {
            // Cache-aside pattern: cache balance for 5 minutes (300 seconds)
            redisTemplate.opsForValue().set(cacheKey, balance.toString(), 300, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("[WARN] Redis is unavailable to set balance cache: " + e.getMessage());
        }
        
        return balance;
    }

    public Account updateBalance(Long accountId, BigDecimal amount) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        account.setBalance(amount);
        Account updatedAccount = accountRepository.save(account);

        try {
            // Invalidate Redis cache on balance update
            String cacheKey = "balance:" + accountId;
            redisTemplate.delete(cacheKey);
        } catch (Exception e) {
            System.err.println("[WARN] Redis is unavailable to delete balance cache: " + e.getMessage());
        }

        return updatedAccount;
    }

}

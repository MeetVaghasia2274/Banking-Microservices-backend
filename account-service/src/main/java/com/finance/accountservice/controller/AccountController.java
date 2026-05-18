package com.finance.accountservice.controller;

import com.finance.accountservice.model.Account;
import com.finance.accountservice.model.AccountType;
import com.finance.accountservice.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<Account> createAccount(@RequestBody Map<String, Object> request) {
        Long userId = Long.valueOf(request.get("userId").toString());
        AccountType accountType = AccountType.valueOf(request.getOrDefault("accountType", "SAVINGS").toString());
        Account account = accountService.createAccount(userId, accountType);
        return ResponseEntity.ok(account);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<Account>> getAccounts(@PathVariable Long userId) {
        List<Account> accounts = accountService.getAccountsByUserId(userId);
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/balance/{accountId}")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable Long accountId) {
        BigDecimal balance = accountService.getBalance(accountId);
        return ResponseEntity.ok(balance);
    }

    @GetMapping("/details/{accountId}")
    public ResponseEntity<Account> getAccountDetails(@PathVariable Long accountId) {
        Account account = accountService.getAccountById(accountId);
        return ResponseEntity.ok(account);
    }

    @GetMapping("/details/number/{accountNumber}")
    public ResponseEntity<Account> getAccountDetailsByNumber(@PathVariable String accountNumber) {
        Account account = accountService.getAccountByAccountNumber(accountNumber);
        return ResponseEntity.ok(account);
    }

    @PutMapping("/balance/{accountId}")
    public ResponseEntity<Account> updateBalance(@PathVariable Long accountId, @RequestBody Map<String, BigDecimal> request) {
        BigDecimal balance = request.get("balance");
        Account updatedAccount = accountService.updateBalance(accountId, balance);
        return ResponseEntity.ok(updatedAccount);
    }
}

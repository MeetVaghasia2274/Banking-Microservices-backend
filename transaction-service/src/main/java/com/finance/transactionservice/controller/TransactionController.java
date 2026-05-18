package com.finance.transactionservice.controller;

import com.finance.transactionservice.model.Transaction;
import com.finance.transactionservice.model.TransferRequest;
import com.finance.transactionservice.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transfer")
    public ResponseEntity<Transaction> transferMoney(@RequestBody TransferRequest request) {
        Transaction transaction = transactionService.transferMoney(request);
        return ResponseEntity.ok(transaction);
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<List<Transaction>> getTransactionHistory(@PathVariable Long accountId) {
        List<Transaction> history = transactionService.getTransactionHistory(accountId);
        return ResponseEntity.ok(history);
    }
}

package com.frauddetection.payment.controller;

import com.frauddetection.common.ApiResponse;
import com.frauddetection.payment.dto.CreateTransactionRequest;
import com.frauddetection.payment.dto.TransactionResponse;
import com.frauddetection.payment.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TransactionResponse> createTransaction(@Valid @RequestBody CreateTransactionRequest request) {
        return ApiResponse.success(transactionService.createTransaction(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<TransactionResponse> getTransaction(@PathVariable UUID id) {
        return ApiResponse.success(transactionService.getTransaction(id));
    }

    @GetMapping("/account/{accountId}")
    public ApiResponse<List<TransactionResponse>> getTransactionsByAccount(@PathVariable UUID accountId) {
        return ApiResponse.success(transactionService.getTransactionsByAccount(accountId));
    }
}

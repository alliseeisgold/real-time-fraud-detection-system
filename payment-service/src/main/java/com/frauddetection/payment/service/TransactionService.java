package com.frauddetection.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.frauddetection.common.TransactionEvent;
import com.frauddetection.common.TransactionStatus;
import com.frauddetection.payment.dto.CreateTransactionRequest;
import com.frauddetection.payment.dto.TransactionResponse;
import com.frauddetection.payment.entity.OutboxEvent;
import com.frauddetection.payment.entity.OutboxEventStatus;
import com.frauddetection.payment.entity.Transaction;
import com.frauddetection.payment.exception.OutboxSerializationException;
import com.frauddetection.payment.exception.TransactionNotFoundException;
import com.frauddetection.payment.repository.OutboxEventRepository;
import com.frauddetection.payment.repository.TransactionRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private static final String RAW_TRANSACTIONS_TOPIC = "transactions.raw";
    private static final String TRANSACTION_AGGREGATE_TYPE = "Transaction";

    private final TransactionRepository transactionRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public TransactionResponse createTransaction(@NonNull CreateTransactionRequest request) {
        Transaction transaction = Transaction.builder()
                .accountId(request.accountId())
                .amount(request.amount())
                .currency(request.currency())
                .country(request.country())
                .merchantCategory(request.merchantCategory())
                .status(TransactionStatus.PENDING)
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);
        OutboxEvent outboxEvent = buildOutboxEvent(savedTransaction);
        outboxEventRepository.save(outboxEvent);

        log.info("Created transaction id={} accountId={} status={}",
                savedTransaction.getId(), savedTransaction.getAccountId(), savedTransaction.getStatus());

        return TransactionResponse.from(savedTransaction);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(@NonNull UUID transactionId) {
        return transactionRepository.findById(transactionId)
                .map(TransactionResponse::from)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionsByAccount(@NonNull UUID accountId) {
        return transactionRepository.findByAccountIdOrderByCreatedAtDesc(accountId)
                .stream()
                .map(TransactionResponse::from)
                .toList();
    }

    private OutboxEvent buildOutboxEvent(Transaction transaction) {
        TransactionEvent event = new TransactionEvent(
                transaction.getId(),
                transaction.getAccountId(),
                transaction.getAmount(),
                transaction.getCurrency().name(),
                transaction.getCountry(),
                transaction.getCreatedAt(),
                transaction.getMerchantCategory()
        );

        try {
            return OutboxEvent.builder()
                    .aggregateType(TRANSACTION_AGGREGATE_TYPE)
                    .aggregateId(transaction.getId())
                    .eventType(TransactionEvent.class.getName())
                    .topic(RAW_TRANSACTIONS_TOPIC)
                    .payload(objectMapper.writeValueAsString(event))
                    .status(OutboxEventStatus.PENDING)
                    .retryCount(0)
                    .build();
        } catch (JsonProcessingException exception) {
            throw new OutboxSerializationException(exception);
        }
    }
}

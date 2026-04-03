package com.frauddetection.fraudanalyzer.service;

import com.frauddetection.common.TransactionEvent;
import com.frauddetection.fraudanalyzer.entity.AccountCountry;
import com.frauddetection.fraudanalyzer.repository.AccountCountryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountCountryService {

    private final AccountCountryRepository accountCountryRepository;

    @Transactional(readOnly = true)
    public boolean isNewCountry(TransactionEvent event) {
        return accountCountryRepository.findById(event.accountId())
                .map(accountCountry -> !accountCountry.getCountry().equalsIgnoreCase(event.country()))
                .orElse(false);
    }

    @Transactional
    public void recordCountry(TransactionEvent event) {
        AccountCountry accountCountry = accountCountryRepository.findById(event.accountId())
                .orElseGet(() -> AccountCountry.builder()
                        .accountId(event.accountId())
                        .build());

        accountCountry.setCountry(event.country().toUpperCase());
        accountCountryRepository.save(accountCountry);
    }
}

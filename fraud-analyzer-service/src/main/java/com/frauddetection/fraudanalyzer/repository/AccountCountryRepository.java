package com.frauddetection.fraudanalyzer.repository;

import com.frauddetection.fraudanalyzer.entity.AccountCountry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccountCountryRepository extends JpaRepository<AccountCountry, UUID> {
}

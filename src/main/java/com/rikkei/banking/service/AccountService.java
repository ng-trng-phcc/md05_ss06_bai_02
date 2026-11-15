package com.rikkei.banking.service;

import com.rikkei.banking.dto.AccountResponse;
import com.rikkei.banking.dto.CreateAccountRequest;
import com.rikkei.banking.entity.Account;
import com.rikkei.banking.exception.AccountNotFoundException;
import com.rikkei.banking.exception.DuplicateAccountNumberException;
import com.rikkei.banking.repository.AccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public AccountResponse create(CreateAccountRequest request) {
        String accountNumber = request.accountNumber().trim();
        if (accountRepository.existsByAccountNumber(accountNumber)) {
            throw new DuplicateAccountNumberException(accountNumber);
        }

        Account account = new Account(accountNumber, request.ownerName().trim(), request.initialBalance());
        try {
            return AccountResponse.from(accountRepository.saveAndFlush(account));
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateAccountNumberException(accountNumber);
        }
    }

    @Transactional(readOnly = true)
    public AccountResponse getById(Long id) {
        return accountRepository.findById(id)
                .map(AccountResponse::from)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> list() {
        return accountRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
                .stream()
                .map(AccountResponse::from)
                .toList();
    }
}

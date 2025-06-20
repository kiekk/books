package com.example.studyspringwebflow.service;

import com.example.studyspringwebflow.entity.Account;
import com.example.studyspringwebflow.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;


@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    @Override
    @Transactional(readOnly = false)
    public Account save(Account account) {
        return this.accountRepository.save(account);
    }

    @Override
    public Account login(String username, String password) throws AuthenticationException {
        Account account = this.accountRepository.findByUsername(username);
        if (account != null) {
            if (!account.getPassword().equalsIgnoreCase(password)) {
                throw new AuthenticationException("Wrong username/password combination.", "invalid.password");
            }
        } else {
            throw new AuthenticationException("Wrong username/password combination.", "invalid.username");
        }

        return account;
    }

    @Override
    public Account getAccount(String username) {
        return this.accountRepository.findByUsername(username);
    }
}

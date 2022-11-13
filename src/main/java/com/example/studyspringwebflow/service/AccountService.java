package com.example.studyspringwebflow.service;

import com.example.studyspringwebflow.entity.Account;

public interface AccountService {

    Account save(Account account);

    Account login(String username, String password) throws AuthenticationException;

    Account getAccount(String username);
}

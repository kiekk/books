package com.example.studyspringwebflow.repository;

import com.example.studyspringwebflow.entity.Account;

public interface AccountRepository {

    Account findByUsername(String username);

    Account findById(long id);

    Account save(Account account);

}

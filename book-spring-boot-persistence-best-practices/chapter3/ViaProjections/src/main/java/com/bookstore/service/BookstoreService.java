package com.bookstore.service;

import com.bookstore.projection.AuthorNameAge;
import com.bookstore.repository.AuthorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class BookstoreService {
    private final AuthorRepository authorRepository;

    public BookstoreService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    public List<AuthorNameAge> fetchFirst2ByBirthplace() {
        return authorRepository.findFirst2ByGenre("Anthology");
    }

    public List<AuthorNameAge> fetchByBirthplace() {
        return authorRepository.fetchByGenre("Anthology");
    }

    public List<AuthorNameAge> fetchAuthorsNamesAndAges() {
        return authorRepository.fetchNameAndAge();
    }

    public List<String> fetchAuthorsNames() {
        return authorRepository.fetchName();
    }
}

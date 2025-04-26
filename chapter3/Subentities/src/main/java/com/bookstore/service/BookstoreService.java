package com.bookstore.service;

import com.bookstore.entity.AuthorDeep;
import com.bookstore.entity.AuthorShallow;
import com.bookstore.repository.AuthorDeepRepository;
import com.bookstore.repository.AuthorShallowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

@Service
public class BookstoreService {
    private final AuthorShallowRepository authorShallowRepository;
    private final AuthorDeepRepository authorDeepRepository;

    public BookstoreService(AuthorShallowRepository authorShallowRepository,
                            AuthorDeepRepository authorDeepRepository) {
        this.authorShallowRepository = authorShallowRepository;
        this.authorDeepRepository = authorDeepRepository;
    }

    @Transactional
    public void createAuthors() throws IOException {
        AuthorDeep mt = AuthorDeep.createAuthorDeep(
                1L,
                43,
                "Martin Ticher",
                "Horror",
                Files.readAllBytes(new File("avatars/mt.png").toPath()));

        AuthorDeep cd = AuthorDeep.createAuthorDeep(
                2L,
                31,
                "Carla Donnoti",
                "Sience Fiction",
                Files.readAllBytes(new File("avatars/cd.png").toPath())
        );

        AuthorDeep re = AuthorDeep.createAuthorDeep(
                3L,
                46,
                "Rennata Elibol",
                "Fantasy",
                Files.readAllBytes(new File("avatars/re.png").toPath())
        );

        authorDeepRepository.save(mt);
        authorDeepRepository.save(cd);
        authorDeepRepository.save(re);
    }

    @Transactional(readOnly = true)
    public List<AuthorShallow> fetchAuthorsShallow() {
        return authorShallowRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<AuthorDeep> fetchAuthorsDeep() {
        return authorDeepRepository.findAll();
    }
}

package com.bookstore.service;

import com.bookstore.entity.Author;
import com.bookstore.repository.AuthorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

@Service
public class BookstoreService {

    private final AuthorRepository authorRepository;

    public BookstoreService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    @Transactional
    public void newAuthor() throws IOException {
        Author mt = Author.createAuthor("Martin Ticher", 43, "Horror");
        mt.setAvatar(Files.readAllBytes(new File("avatars/mt_avatar.png").toPath()));
        mt.setBiography(Files.readString(new File("biography/mt_bio.txt").toPath()));

        authorRepository.save(mt);
    }

    @Transactional(readOnly = true)
    public void fetchAuthor() {
        Author author = authorRepository.findByName("Martin Ticher");
        System.out.println("Author bio: " + author.getBiography());
        System.out.println("Author avatar: " + Arrays.toString(author.getAvatar()));
    }
}

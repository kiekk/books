package com.bookstore.service;

import com.bookstore.entity.Author;
import com.bookstore.repository.AuthorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

@Service
public class BookstoreService {

    private final AuthorRepository authorRepository;

    public BookstoreService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    @Transactional
    public void createAuthors() throws IOException {
        Author mt = Author.createAuthor(
                1L,
                "Martin Ticher",
                43,
                "Horror",
                Files.readAllBytes(new File("avatars/mt.png").toPath()));
        Author cd = Author.createAuthor(
                2L,
                "Carla Donnoti",
                31,
                "Science Fiction",
                Files.readAllBytes(new File("avatars/cd.png").toPath()));
        Author re = Author.createAuthor(
                3L,
                "Rennata Elibol",
                46,
                "Fantasy",
                Files.readAllBytes(new File("avatars/re.png").toPath()));

        authorRepository.save(mt);
        authorRepository.save(cd);
        authorRepository.save(re);
    }

    @Transactional(readOnly = true)
    public List<Author> fetchAuthorsByAgeGreaterThanEqual(int age) {
        return authorRepository.findByAgeGreaterThanEqual(age);
    }

    @Transactional(readOnly = true)
    public byte[] fetchAuthorAvatarViaId(long id) {
        Author author = authorRepository.findById(id).orElseThrow();
        return author.getAvatar();  // lazy loading of 'avatar'
    }

    @Transactional(readOnly = true)
    public List<Author> fetchAuthorsDetailsByAgeGreaterThanEqual(int age) {
        List<Author> authors = authorRepository.findByAgeGreaterThanEqual(age);

        // don't do this since this is a N+1 case
        authors.forEach(Author::getAvatar);

        return authors;
    }

}

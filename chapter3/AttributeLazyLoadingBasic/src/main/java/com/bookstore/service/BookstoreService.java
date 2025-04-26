package com.bookstore.service;

import com.bookstore.dto.AuthorDto;
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
    public Author fetchAuthor(long id) {
        Author author = authorRepository.findById(id).orElseThrow();

        // avatar 초기화
        // api 엔드포인트에서 entity를 그대로 반환하면 초기화 되지 않은 Lazy Loading 필드에 대해 에러가 발생한다.
        // 따라서 아래와 같이 명시적으로 초기화 해준다.
        // 추가로 값이 없는 avatar를 직렬화에서 제외하고 싶다면 Author entity에 @JSONInclude를 사용하여 직렬화에서 제외하도록 해야 한다.
        author.initializeAvatar();

        return author;
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

    @Transactional(readOnly = true)
    public List<AuthorDto> fetchAuthorsWithAvatarsByAgeGreaterThanEqual(int age) {
        return authorRepository.findDtoByAgeGreaterThanEqual(40);
    }

}

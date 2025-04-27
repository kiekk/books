package com.bookstore.service;

import com.bookstore.dto.BookstoreDto;
import com.bookstore.repository.AuthorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BookstoreService {
    private final AuthorRepository authorRepository;

    public BookstoreService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    @Transactional
    public List<BookstoreDto> fetchAuthors() {
        List<BookstoreDto> dto = authorRepository.fetchAll();

        // @Transactional(readOnly = false)일 경우
        // 조회한 Author는 하이버네이트에 의해 관리되기 때문에 더티 체킹이 활성화 됩니다.
        // @Transactional(readOnly = true)일 경우
        // 조회한 Author는 하이버네이트에 의해 관리되지 않기 때문에 더티 체킹이 비활성화 됩니다.
        dto.getFirst().getAuthor().setGenre("Poetry");

        return dto;
    }
}

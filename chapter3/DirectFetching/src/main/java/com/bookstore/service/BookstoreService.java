package com.bookstore.service;

import com.bookstore.dao.Dao;
import com.bookstore.entity.Author;
import com.bookstore.repository.AuthorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class BookstoreService {
    private final AuthorRepository authorRepository;
    private final Dao<Author, Long> dao;

    public BookstoreService(AuthorRepository authorRepository, Dao<Author, Long> dao) {
        this.authorRepository = authorRepository;
        this.dao = dao;
    }

    /*
    @Transactional이 없으면 각 find 호출마다 새로운 영속성 컨텍스트가 생성되어 3번의 SELECT 쿼리가 발생합니다.
    @Transactional이 없으면, 각 메서드 호출(findById, dao.find, dao.findViaSession) 시 각각 새로운 EntityManager나 Session이 생성됩니다.

    @Transactional(readOnly = true)를 사용하면 같은 트랜잭션 내에서 하나의 영속성 컨텍스트가 공유되어, 같은 ID로 조회하는 경우 1번만 SELECT 쿼리가 발생합니다 (나머지는 캐시에서 가져옴).
    @Transactional을 사용하면, 트랜잭션이 시작되며 해당 메서드 내에서는 하나의 EntityManager가 공유됩니다.
     */
    @Transactional(readOnly = true)
    public void directFetching() {
        // direct fetching via Spring Data
        Optional<Author> resultSD = authorRepository.findById(1L);
        System.out.println("Direct fetching via Spring Data result: " + resultSD.get());

        // direct fetching via EntityManager
        Optional<Author> resultEM = dao.find(Author.class, 1L);
        System.out.println("Direct fetching via EntityManager result: " + resultEM.get());

        // direct fetching via Session
        Optional<Author> resultHS = dao.findViaSession(Author.class, 1L);
        System.out.println("Direct fetching via Session result: " + resultHS.get());
    }
}

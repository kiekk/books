package com.bookstore.service;

import com.bookstore.dao.Dao;
import com.bookstore.entity.Author;
import com.bookstore.repository.AuthorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

@Service
public class BookstoreService {
    private final AuthorRepository authorRepository;
    private final TransactionTemplate template;
    private final Dao<Author, Long> dao;

    public BookstoreService(AuthorRepository authorRepository, TransactionTemplate template, Dao<Author, Long> dao) {
        this.authorRepository = authorRepository;
        this.template = template;
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

    public void process() {
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        // READ_COMMITTED를 사용하여 Hibernate 세션 수준 반복 읽기 동작을 확인합니다.
        template.setIsolationLevel(Isolation.READ_COMMITTED.value());

        // Transaction A
        template.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                Author authorA1 = authorRepository.findById(1L).orElseThrow();
                System.out.println("Author A1: " + authorA1.getName() + "\n");

                // Transaction B
                template.execute(new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus status) {

                        // 트랜잭션 A에서 Author를 조회하여도 트랜잭션 B에서 다시 Author를 조회합니다.
                        Author authorB = authorRepository.findById(1L).orElseThrow();
                        authorB.setName("Alicia Tom");
                        System.out.println("Author B: " + authorB.getName() + "\n");
                    }
                });

                /*
                트랜잭션 B에서 Author를 수정하여도 트랜잭션 A에서 조회한 Author를 반환합니다.
                 */
                // 영속성 컨텍스트를 먼저 확인하여 엔티티가 있을 경우 추가 SELECT 쿼리를 실행하지 않음
                Author authorA2 = authorRepository.findById(1L).orElseThrow();
                System.out.println("\nAuthor A2: " + authorA2.getName() + "\n");

                // JPQL 엔터티 쿼리는 세션 수준 반복 읽기의 장점을 활용하여 SELECT 쿼리는 실행되지만
                // SELECT로부터의 데이터 스냅샷은 무시합니다.
                Author authorViaJpql = authorRepository.fetchByIdJpql(1L);
                System.out.println("Author via JPQL: " + authorViaJpql.getName() + "\n");

                // SQL 엔터티 쿼리는 세션 수준 반복 읽기의 장점을 활용하여 SELECT 쿼리는 실행되지만
                // SELECT로부터의 데이터 스냅샷은 무시합니다.
                Author authorViaSql = authorRepository.fetchByIdSql(1L);
                System.out.println("Author via SQL: " + authorViaSql.getName() + "\n");

                /*
                DB에서 조회한 데이터 스냅샷을 무시하지 않기 때문에 트랜잭션 B에서 수정한 Author가 조회됩니다.
                하지만 영속성 컨텍스트의 Author를 대체하지는 않습니다.
                 */
                // JPQL 쿼리 프로젝션은 항상 최종 데이터베이스 상태를 로드합니다.
                String nameViaJpql = authorRepository.fetchNameByIdJpql(1L);
                System.out.println("Author name via JPQL: " + nameViaJpql + "\n");

                // SQL 쿼리 프로젝션은 항상 최종 데이터베이스 상태를 로드합니다.
                String nameViaSql = authorRepository.fetchNameByIdSql(1L);
                System.out.println("Author name via SQL: " + nameViaSql + "\n");

                /*
                여전히 트랜잭션 A의 영속성 컨텍스트에 있는 Author가 조회됩니다.
                 */
                Author authorViaSql2 = authorRepository.fetchByIdSql(1L);
                System.out.println("Author via SQL2: " + authorViaSql2.getName() + "\n");
            }
        });
    }
}

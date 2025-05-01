package com.bookstore.service;

import com.bookstore.dto.AuthorDto;
import com.bookstore.entity.Author;
import com.bookstore.repository.AuthorRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class BookstoreService {

    private final AuthorRepository authorRepository;
    private final EntityManager entityManager;

    public BookstoreService(AuthorRepository authorRepository, EntityManager entityManager) {
        this.authorRepository = authorRepository;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public void fetchAuthorAsReadOnlyEntities() {
        List<Author> authors = authorRepository.findAll();
        authors.forEach(System.out::println);

        briefOverviewOfPersistentContextContent();
    }

    @Transactional(readOnly = true)
    public void fetchAuthorAsProjection() {
        List<AuthorDto> authors = authorRepository.findBy();
        authors.forEach(author -> System.out.println("Author{" +
                "id=" + author.getId() +
                ", name='" + author.getName() + '\'' +
                ", genre='" + author.getGenre() + '\'' +
                ", age=" + author.getAge() +
                '}'));
        // 같은 findBy 메서드라도 엔터티 모든 컬럼을 조회한 후 Object[]로 반환받을 경우 영속성 컨텍스트에 데이터가 로드됩니다.
        /*
        1.
        List<Object[]> findBy(); --> 영속성 컨텍스트에 로드 됨

        2. @Query를 사용하여 엔터티 전체 조회
        @Query("SELECT a FROM Author a")
        List<Object[]> findBy(); --> 영속성 컨텍스트에 로드 됨

        2. @Query를 사용하여 컬럼을 조회 조회
        @Query("SELECT a.age, a.name FROM Author a")
        @Query("SELECT a.id, a.age, a.name, a.genre FROM Author a") // 모든 컬럼을 조회해도 영속성 컨텍스트에 로드 되지 않음
        List<Object[]> findBy(); --> 영속성 컨텍스트에 로드 되지 않음
         */
//        List<Object[]> authors = authorRepository.findBy();
//        authors.forEach(a -> System.out.println(Arrays.toString(a)));

        briefOverviewOfPersistentContextContent();
    }

    @Transactional(readOnly = true)
    public void fetchAuthorAsArrayOfObject() {
        // 영속성 컨텍스트에 로드 됨
        List<Object[]> authors = authorRepository.fetchAsArray();
        authors.forEach(a -> System.out.println(Arrays.toString(a)));

        briefOverviewOfPersistentContextContent();
    }

    private void briefOverviewOfPersistentContextContent() {
        org.hibernate.engine.spi.PersistenceContext persistenceContext = getPersistenceContext();

        int managedEntities = persistenceContext.getNumberOfManagedEntities();

        System.out.println("\n-----------------------------------");
        System.out.println("Total number of managed entities: " + managedEntities);

        // getEntitiesByKey() will be removed and probably replaced with #iterateEntities()
        Map<EntityKey, Object> entitiesByKey = persistenceContext.getEntitiesByKey();
        entitiesByKey.forEach((key, value) -> System.out.println(key + ": " + value));

        for (Object entry : entitiesByKey.values()) {
            EntityEntry ee = persistenceContext.getEntry(entry);
            System.out.println("Entity name: " + ee.getEntityName()
                    + " | Status: " + ee.getStatus()
                    + " | State: " + Arrays.toString(ee.getLoadedState()));
        }

        System.out.println("\n-----------------------------------\n");
    }

    private org.hibernate.engine.spi.PersistenceContext getPersistenceContext() {
        SharedSessionContractImplementor sharedSession = entityManager.unwrap(
                SharedSessionContractImplementor.class
        );

        return sharedSession.getPersistenceContext();
    }
}

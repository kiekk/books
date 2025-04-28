package com.bookstore.service;

import com.bookstore.dto.BookDto;
import com.bookstore.repository.BookRepository;
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

    private final BookRepository bookRepository;
    private final EntityManager entityManager;

    public BookstoreService(BookRepository bookRepository, EntityManager entityManager) {
        this.bookRepository = bookRepository;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public List<BookDto> fetchBooksWithAuthorsQueryBuilderMechanism() {
        List<BookDto> books = bookRepository.findBy();

        System.out.println("\nResult set:");
        books.forEach(b -> System.out.println(b.getTitle() + ", "
                + b.getAuthor().getName() + ", " + b.getAuthor().getGenre()));

        briefOverviewOfPersistentContextContent();

        return books;
    }

    private void briefOverviewOfPersistentContextContent() {
        org.hibernate.engine.spi.PersistenceContext persistenceContext = getPersistenceContext();

        int managedEntities = persistenceContext.getNumberOfManagedEntities();
        int collectionEntriesSize = persistenceContext.getCollectionEntriesSize();

        System.out.println("\n-----------------------------------");
        System.out.println("Total number of managed entities: " + managedEntities);
        System.out.println("Total number of collection entries: " + collectionEntriesSize + "\n");

        // getEntitiesByKey() will be removed and probably replaced with #iterateEntities()
        Map<EntityKey, Object> entitiesByKey = persistenceContext.getEntitiesByKey();
        entitiesByKey.forEach((key, value) -> System.out.println(key + ":" + value));

        for (Object entry : entitiesByKey.values()) {
            EntityEntry ee = persistenceContext.getEntry(entry);
            System.out.println(
                    "Entity name: " + ee.getEntityName()
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

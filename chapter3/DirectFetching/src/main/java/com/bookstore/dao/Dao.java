package com.bookstore.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class Dao<T, ID extends Serializable> implements GenericDao<T, ID> {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<T> find(Class<T> clazz, ID id) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }

        return Optional.ofNullable(entityManager.find(clazz, id));
    }
}

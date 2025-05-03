package com.bookstore.dao;

import com.bookstore.dto.AuthorDtoNoSetters;
import com.bookstore.dto.AuthorDtoWithSetters;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.hibernate.transform.AliasToBeanConstructorResultTransformer;
import org.hibernate.transform.Transformers;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class Dao implements AuthorDao {

    @PersistenceContext
    EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public List<AuthorDtoNoSetters> fetchAuthorsNoSetters() {
        Query query = entityManager
                .createNativeQuery("SELECT name, age FROM author")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setResultTransformer(
                        new AliasToBeanConstructorResultTransformer(
                                AuthorDtoNoSetters.class.getConstructors()[0]
                        )
                );

        List<AuthorDtoNoSetters> authors = query.getResultList();

        return authors;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuthorDtoWithSetters> fetchAuthorsWithSetters() {
        Query query = entityManager
                .createNativeQuery("SELECT name, age FROM author")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .addScalar("name") // 미적용시 쿼리 결과를 NAME으로 반환하여 오류 발생 Could not resolve PropertyAccess for NAME on class com.bookstore.dto.AuthorDtoWithSetters
                .addScalar("age") // 미적용시 쿼리 결과를 AGE로 반환하여 오류 발생 Could not resolve PropertyAccess for AGE on class com.bookstore.dto.AuthorDtoWithSetters
                .setResultTransformer(
                        Transformers.aliasToBean(AuthorDtoWithSetters.class)
                );

        List<AuthorDtoWithSetters> authors = query.getResultList();

        return authors;
    }

    protected EntityManager getEntityManager() {
        return entityManager;
    }
}

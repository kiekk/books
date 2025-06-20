package com.bookstore.builder;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

public class SpecificationChunk<T> implements Specification<T> {
    private final Condition condition;

    public SpecificationChunk(Condition condition) {
        this.condition = condition;
    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> cquery, CriteriaBuilder cbuilder) {
        switch (condition.getOperation()) {
            case EQUAL:
                return cbuilder.equal(
                        root.get(condition.getLeftHand()), condition.getRightHand());
            case NOT_EQUAL:
                return cbuilder.notEqual(
                        root.get(condition.getLeftHand()), condition.getRightHand());
            case GREATER_THAN:
                return cbuilder.greaterThan(
                        root.get(condition.getLeftHand()), condition.getRightHand());
            case LESS_THAN:
                return cbuilder.lessThan(
                        root.get(condition.getLeftHand()), condition.getRightHand());
            case LIKE:
                return cbuilder.like(
                        root.get(condition.getLeftHand()), condition.getRightHand());
            default:
                return null;
        }
    }
}

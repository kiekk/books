package com.bookstore.builder;

import com.bookstore.builder.Condition.OperationType;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

import static com.bookstore.builder.Condition.*;

public class SpecificationBuilder<T> {
    private final List<Condition> conditions;

    public SpecificationBuilder() {
        conditions = new ArrayList<>();
    }

    public SpecificationBuilder<T> with(String leftHand, String rightHand, OperationType operation, LogicalOperatorType operator) {
        conditions.add(new Condition(leftHand, rightHand, operation, operator));
        return this;
    }

    public Specification<T> build() {
        if (conditions.isEmpty()) {
            return null;
        }

        List<Specification<T>> specifications = new ArrayList<>();
        for (Condition condition : conditions) {
            specifications.add(new SpecificationChunk<>(condition));
        }

        Specification<T> finalSpecification = specifications.getFirst();
        for (int i = 1; i < conditions.size(); i++) {
            if (!conditions.get(i - 1).getOperator().equals(LogicalOperatorType.END)) {
                finalSpecification = conditions.get(i - 1).getOperator().equals(LogicalOperatorType.OR)
                        ? finalSpecification.or(specifications.get(i))
                        : finalSpecification.and(specifications.get(i));
            }
        }

        return finalSpecification;
    }
}

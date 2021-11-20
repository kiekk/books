package com.example.recipe23.sequence;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.PrimitiveIterator;
import java.util.concurrent.atomic.AtomicInteger;

public class SequenceGenerator {

    @Autowired
    private PrefixGenerator[] prefixGenerators;
//    Collection 도 가능
//    private List<PrimitiveIterator> prefixGenerators;
//    private Map<String, PrefixGenerator> prefixGenerators;
    private String suffix;
    private int initial;
    private final AtomicInteger counter = new AtomicInteger();

    public SequenceGenerator() {
    }

    public SequenceGenerator(PrefixGenerator[] prefixGenerators, String suffix, int initial) {
        this.prefixGenerators = prefixGenerators;
        this.suffix = suffix;
        this.initial = initial;
    }

    public void setPrefixGenerator(PrefixGenerator[] prefixGenerators) {
        this.prefixGenerators = prefixGenerators;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public void setInitial(int initial) {
        this.initial = initial;
    }

    public String getSequence() {
        StringBuilder builder = new StringBuilder();
        for (PrefixGenerator prefix : prefixGenerators) {
            builder.append(prefix.getPrefix());
            builder.append("-");
        }
        builder.append(initial).append(counter.getAndIncrement()).append(suffix);
        return builder.toString();
    }
}
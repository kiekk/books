package org.example.hacking;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class InventoryService {

    private final ItemRepository repository;

    public InventoryService(ItemRepository repository) {
        this.repository = repository;
    }

    Flux<Item> searchByExample(String name, String description, boolean useAnd) {
        Item item = new Item(name, description, 0.0);

        ExampleMatcher matcher = (useAnd
                ? ExampleMatcher.matchingAll()
                : ExampleMatcher.matchingAny())
                .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING)
                .withIgnoreCase()
                .withIgnorePaths("price");

        Example<Item> probe = Example.of(item, matcher);

        return repository.findAll(probe);
    }

}

package org.example.hacking;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

// ReactiveCrudRepository 의 리턴 타입은 모두 Mono, Flux 둘 중 하나다.
public interface ItemRepository extends ReactiveCrudRepository<Item, String> {
}

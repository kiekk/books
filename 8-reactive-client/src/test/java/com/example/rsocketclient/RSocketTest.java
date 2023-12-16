package com.example.rsocketclient;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureWebTestClient
public class RSocketTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ItemRepository itemRepository;

    @Test
    void verifyREmoteOperationsThroughRSocketRequestResponse() throws InterruptedException {
        // 데이터 초기화
        itemRepository.deleteAll()
                .as(StepVerifier::create)
                .verifyComplete();

        // 새 item 생성
        webTestClient.post()
                .uri("/items/request-response")
                .bodyValue(new Item("Alf alarm clock", "nothing important", 19.99))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Item.class)
                .value(item -> {
                    assertThat(item.getId()).isNotNull();
                    assertThat(item.getName()).isEqualTo("Alf alarm clock");
                    assertThat(item.getDescription()).isEqualTo("nothing important");
                    assertThat(item.getPrice()).isEqualTo(19.99);
                });

        Thread.sleep(500);

        // 저장 확인
        itemRepository.findAll()
                .as(StepVerifier::create)
                .expectNextMatches(item -> {
                    assertThat(item.getId()).isNotNull();
                    assertThat(item.getName()).isEqualTo("Alf alarm clock");
                    assertThat(item.getDescription()).isEqualTo("nothing important");
                    assertThat(item.getPrice()).isEqualTo(19.99);
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void verifyRemoteOperationsThroughRSocketRequestStream() {
        // 데이터 초기화
        itemRepository.deleteAll().block();

        // Item 3개 생성
        List<Item> items = IntStream.rangeClosed(1, 3)
                .mapToObj(i -> new Item("name - " + i, "description - " + i, i))
                .toList();

        itemRepository.saveAll(items).blockLast();

        // 스트림 테스트
        webTestClient.get()
                .uri("/items/request-stream")
                .accept(MediaType.APPLICATION_NDJSON)
                .exchange()
                .expectStatus().isOk()
                .returnResult(Item.class)
                .getResponseBody()
                .as(StepVerifier::create)
                .expectNextMatches(itemPredicate("1"))
                .expectNextMatches(itemPredicate("2"))
                .expectNextMatches(itemPredicate("3"))
                .verifyComplete();
    }

    @Test
    void verifyRemoteOperationsThroughRSocketFireAndForget() throws InterruptedException {
        // 데이터 초기화
        itemRepository.deleteAll()
                .as(StepVerifier::create)
                .verifyComplete();

        // 새 item 생성
        webTestClient.post()
                .uri("/items/fire-and-forget")
                .bodyValue(new Item("Alf alarm clock", "nothing important", 19.99))
                .exchange()
                .expectStatus().isCreated()
                .expectBody().isEmpty();

        Thread.sleep(500);

        // 저장 확인
        itemRepository.findAll()
                .as(StepVerifier::create)
                .expectNextMatches(item -> {
                    assertThat(item.getId()).isNotNull();
                    assertThat(item.getName()).isEqualTo("Alf alarm clock");
                    assertThat(item.getDescription()).isEqualTo("nothing important");
                    assertThat(item.getPrice()).isEqualTo(19.99);
                    return true;
                })
                .verifyComplete();
    }

    private Predicate<Item> itemPredicate(String number) {
        return item -> {
            assertThat(item.getName()).startsWith("name");
            assertThat(item.getName()).endsWith(number);
            assertThat(item.getDescription()).startsWith("description");
            assertThat(item.getDescription()).endsWith(number);
            assertThat(item.getPrice()).isPositive();
            return true;
        };
    }
}

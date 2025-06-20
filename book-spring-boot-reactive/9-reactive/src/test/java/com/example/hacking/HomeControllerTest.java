package com.example.hacking;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureWebTestClient
public class HomeControllerTest {

    @Autowired
    WebTestClient webTestClient;

    @Autowired
    ItemRepository itemRepository;

    @Test
    void verifyLoginPageBlocksAccess() {
        webTestClient.get()
                .uri("/")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @WithMockUser(username = "soono")
    void verifyLoginPageWorks() {
        webTestClient.get()
                .uri("/")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @WithMockUser(username = "alice", roles = {"SOME_OTHER_ROLE"})
    void addingInventoryWithoutProperRoleFails() {
        webTestClient.post()
                .uri("/")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @WithMockUser(username = "soono", roles = {"INVENTORY"})
    @DisplayName("현재 expectNextMatches에서 에러 발생, 추후 해결해야함")
    void addingInventoryWithProperRoleSucceeds() {
        webTestClient
                .post()
                .uri("/")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{" +
                        "\"name\": \"iPhone 11\", " +
                        "\"description\": \"upgrade\", " +
                        "\"price\": 999.99" +
                        "}")
                .exchange()
                .expectStatus().isOk();

        itemRepository.findByName("iPhone 11")
                .as(StepVerifier::create)
                .expectNextMatches(item -> {
                    assertThat(item.getDescription()).isEqualTo("upgrade");
                    assertThat(item.getPrice()).isEqualTo(999.99);
                    return true;
                })
                .verifyComplete();
    }

    @Test
    @WithMockUser(username = "carol", roles = {"SOME_OTHER_ROLE"})
    void deletingInventoryWithoutProperRoleFails() {
        webTestClient
                .delete().uri("/some-item")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @WithMockUser(username = "dan", roles = {"INVENTORY"})
    void deletingInventoryWithProperRoleSucceeds() {
        String id = itemRepository.findByName("Alf alarm clock")
                .map(Item::getId)
                .block();

        webTestClient
                .delete()
                .uri("/" + id)
                .exchange()
                .expectStatus().isOk();

        itemRepository.findByName("Alf alarm clock")
                .as(StepVerifier::create)
                .expectNextCount(0)
                .verifyComplete();
    }
}

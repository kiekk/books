package com.example.hacking;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;

@WebFluxTest(controllers = ApiItemController.class)
@AutoConfigureRestDocs
class ApiItemControllerDocumentationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ItemRepository itemRepository;

    @Test
    void findingAllItems() {
        when(itemRepository.findAll())
                .thenReturn(Flux.just(createItem("item-1")));

        webTestClient.get()
                .uri("/api/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(document("findAll", preprocessResponse(prettyPrint())));
    }

    @Test
    void postNewItem() {
        when(itemRepository.save(any()))
                .thenReturn(Mono.just(createItem("item-1")));

        webTestClient.post()
                .uri("/api/items")
                .bodyValue(createItem())
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .consumeWith(document("post-new-item", preprocessResponse(prettyPrint())));
    }

    @Test
    void findOneItem() {
        when(itemRepository.findById("item-1"))
                .thenReturn(Mono.just(createItem("item-1")));

        webTestClient.get().uri("/api/items/item-1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(document("findOne", preprocessResponse(prettyPrint())));
    }

    @Test
    void updateItem() {
        when(itemRepository.save(any())).thenReturn(
                Mono.just(createItem("item-1")));

        webTestClient.put().uri("/api/items/item-1")
                .bodyValue(createItem())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(document("update-item", preprocessResponse(prettyPrint())));
    }

    private Item createItem(String id) {
        return new Item(id, "Alf alarm clock", "nothing I really need", 19.99);
    }

    private Item createItem() {
        return createItem(null);
    }
}

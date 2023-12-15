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
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;

@WebFluxTest(controllers = HypermediaItemController.class)
@AutoConfigureRestDocs
public class HypermediaItemControllerDocumentationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ItemRepository itemRepository;

    @Test
    void hyperFindingAllItems() {
        when(itemRepository.findAll())
                .thenReturn(Flux.just(createItem()));
        when(itemRepository.findById((String) null))
                .thenReturn(Mono.just(createItem("item-1")));

        webTestClient.get()
                .uri("/hypermedia/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(document("findAll-hypermedia", preprocessResponse(prettyPrint())));
    }

    @Test
    void hyperPostNewItem() {
        when(itemRepository.save(any())).thenReturn(
                Mono.just(createItem("item-1")));

        when(itemRepository.findById("item-1"))
                .thenReturn(Mono.just(createItem("item-1")));

        webTestClient.post()
                .uri("/hypermedia/items")
                .bodyValue(createItem())
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .consumeWith(document("post-new-item-hypermedia", preprocessResponse(prettyPrint())));;
    }

    @Test
    void hyperFindOneItem() {
        when(itemRepository.findById("item-1"))
                .thenReturn(Mono.just(createItem("item-1")));

        webTestClient.get()
                .uri("/hypermedia/items/item-1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(document("findOne-hypermedia", preprocessResponse(prettyPrint()),
                        links(
                                linkWithRel("self").description("이 `Item`에 대한 공식 링크"),
                                linkWithRel("item").description("`Item` 목록 링크"))));
    }

    @Test
    void hyperFindProfile() {
        webTestClient.get()
                .uri("/hypermedia/items/profile")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(document("profile", preprocessResponse(prettyPrint())));
    }

    private Item createItem(String id) {
        return new Item(id, "Alf alarm clock", "nothing I really need", 19.99);
    }

    private Item createItem() {
        return createItem(null);
    }
}

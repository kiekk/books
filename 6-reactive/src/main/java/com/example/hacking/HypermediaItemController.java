package com.example.hacking;

import org.springframework.hateoas.*;
import org.springframework.hateoas.mediatype.alps.Alps;
import org.springframework.hateoas.mediatype.alps.Type;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import static org.springframework.hateoas.mediatype.alps.Alps.alps;
import static org.springframework.hateoas.mediatype.alps.Alps.descriptor;
import static org.springframework.hateoas.server.reactive.WebFluxLinkBuilder.linkTo;
import static org.springframework.hateoas.server.reactive.WebFluxLinkBuilder.methodOn;

@RestController
public class HypermediaItemController {

    private final ItemRepository itemRepository;

    public HypermediaItemController(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @GetMapping("/hypermedia")
    Mono<RepresentationModel<?>> root() {
        HypermediaItemController controller = methodOn(HypermediaItemController.class);

        Mono<Link> selfLink = linkTo(controller.root()).withSelfRel().toMono();

        Mono<Link> itemsAggregateLink =
                linkTo(controller.findAll())
                        .withRel(IanaLinkRelations.ITEM)
                        .toMono();

        return selfLink.zipWith(itemsAggregateLink)
                .map(links -> Links.of(links.getT1(), links.getT2()))
                .map(links -> new RepresentationModel<>(links.toList()));
    }

    @GetMapping("/hypermedia/items")
    Mono<CollectionModel<EntityModel<Item>>> findAll() {
        return itemRepository.findAll()
                .flatMap(item -> findOne(item.getId()))
                .collectList()
                .flatMap(entityModels -> linkTo(methodOn(HypermediaItemController.class)
                        .findAll()).withSelfRel()
                        .toMono()
                        .map(selfLink -> CollectionModel.of(entityModels, selfLink)));
    }

    @GetMapping("/hypermedia/items/{id}")
    Mono<EntityModel<Item>> findOne(@PathVariable String id) {
        HypermediaItemController controller = methodOn(HypermediaItemController.class);

        Mono<Link> selfLink = linkTo(controller.findOne(id)).withSelfRel().toMono();

        Mono<Link> aggregateLink = linkTo(controller.findAll())
                .withRel(IanaLinkRelations.ITEM).toMono();

        return Mono.zip(itemRepository.findById(id), selfLink, aggregateLink)
                .map(o -> EntityModel.of(o.getT1(), Links.of(o.getT2(), o.getT3())));
    }

    @GetMapping("/hypermedia/items/{id}/affordances")
    Mono<EntityModel<Item>> findOneWithAffordances(@PathVariable String id) {
        HypermediaItemController controller =
                methodOn(HypermediaItemController.class);

        Mono<Link> selfLink = linkTo(controller.findOne(id)).withSelfRel()
                .andAffordance(controller.updateItem(null, id))
                .toMono();

        Mono<Link> aggregateLink = linkTo(controller.findAll()).withRel(IanaLinkRelations.ITEM)
                .toMono();

        return Mono.zip(itemRepository.findById(id), selfLink, aggregateLink)
                .map(o -> EntityModel.of(o.getT1(), Links.of(o.getT2(), o.getT3())));
    }

    @PostMapping("/hypermedia/items")
    Mono<ResponseEntity<?>> addNewItem(@RequestBody Mono<EntityModel<Item>> item) {
        return item
                .map(EntityModel::getContent)
                .flatMap(itemRepository::save)
                .map(Item::getId)
                .flatMap(this::findOne)
                .map(newModel -> ResponseEntity.created(newModel
                        .getRequiredLink(IanaLinkRelations.SELF)
                        .toUri()).build());
    }

    @PutMapping("/hypermedia/items/{id}")
    public Mono<ResponseEntity<?>> updateItem(@RequestBody Mono<EntityModel<Item>> item,
                                              @PathVariable String id) {
        return item
                .map(EntityModel::getContent)
                .map(content -> new Item(id, content.getName(),
                        content.getDescription(), content.getPrice()))
                .flatMap(itemRepository::save)
                .then(findOne(id))
                .map(model -> ResponseEntity.noContent()
                        .location(model.getRequiredLink(IanaLinkRelations.SELF).toUri()).build());
    }

    @GetMapping(value = "/hypermedia/items/profile", produces = MediaTypes.ALPS_JSON_VALUE)
    public Alps profile() {
        return alps()
                .descriptor(Collections.singletonList(descriptor()
                        .id(Item.class.getSimpleName() + "-repr")
                        .descriptor(Arrays.stream(
                                        Item.class.getDeclaredFields())
                                .map(field -> descriptor()
                                        .name(field.getName())
                                        .type(Type.SEMANTIC)
                                        .build())
                                .collect(Collectors.toList()))
                        .build()))
                .build();
    }
}
package org.example.hacking;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

@Controller
public class HomeController {

    private final ItemRepository itemRepository;
    private final CartRepository cartRepository;
    private final CartService cartService;
    private final InventoryService inventoryService;

    public HomeController(ItemRepository itemRepository, CartRepository cartRepository, CartService cartService, InventoryService inventoryService) {
        this.itemRepository = itemRepository;
        this.cartRepository = cartRepository;
        this.cartService = cartService;
        this.inventoryService = inventoryService;
    }

    @GetMapping
    Mono<Rendering> home() {
        return Mono.just(Rendering.view("home.html")
                .modelAttribute("items",
                        itemRepository.findAll())
                .modelAttribute("cart",
                        cartRepository.findById("My Cart")
                                .defaultIfEmpty(new Cart("My Cart")))
                .build());
    }

    @PostMapping("/add/{id}")
    Mono<String> addToCart(@PathVariable String id) {
        return cartService.addToCart("My Cart", id)
                .thenReturn("redirect:/");
    }

    @GetMapping("/search")
    Mono<Rendering> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam boolean useAnd) {
        return Mono.just(Rendering.view("home.html")
                .modelAttribute("items",
                        inventoryService.searchByExample(name, description, useAnd))
                .modelAttribute("cart",
                        cartRepository.findById("My Cart")
                                .defaultIfEmpty(new Cart("My Cart")))
                .build());
    }

}

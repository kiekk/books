package org.example.hacking;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

@Controller
public class HomeController {

    private final ItemRepository itemRepository;
    private final CartRepository cartRepository;
    private final CartService cartService;

    public HomeController(ItemRepository itemRepository, CartRepository cartRepository, CartService cartService) {
        this.itemRepository = itemRepository;
        this.cartRepository = cartRepository;
        this.cartService = cartService;
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

}

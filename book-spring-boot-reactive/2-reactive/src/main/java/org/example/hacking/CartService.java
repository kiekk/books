package org.example.hacking;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class CartService {

    private final ItemRepository itemRepository;
    private final CartRepository cartRepository;

    CartService(ItemRepository itemRepository, CartRepository cartRepository) {
        this.itemRepository = itemRepository;
        this.cartRepository = cartRepository;
    }

    Mono<Cart> addToCart(String cartId, String id) {
        return cartRepository.findById("My Cart")
                .log("foundCart")
                .defaultIfEmpty(new Cart("My Cart"))
                .log("emptyCart")
                .flatMap(cart -> cart.getCartItems().stream()
                        .filter(cartItem -> cartItem.getItem()
                                .getId().equals(id))
                        .findAny()
                        .map(cartItem -> {
                            cartItem.increment();
                            return Mono.just(cart);
                        })
                        .orElseGet(() -> itemRepository.findById(id)
                                .log("fetchedItem")
                                .map(CartItem::new)
                                .log("cartItem")
                                .doOnNext(cartItem -> cart.getCartItems().add(cartItem))
                                .map(cartItem -> cart)
                                .log("addedCartItem"))
                )
                .log("cartWithAnotherItem")
                .flatMap(cartRepository::save)
                .log("savedCart");
    }

}

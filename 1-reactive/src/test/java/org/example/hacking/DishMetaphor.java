package org.example.hacking;

import reactor.core.publisher.Flux;

public class DishMetaphor {

    static class KitchenService {

        Flux<Dish> getDishes() {
            // You could model a ChefService, but let's just
            // hard code some tasty dishes.
            return Flux.just( //
                    new Dish("Sesame chicken"), //
                    new Dish("Lo mein noodles, plain"), //
                    new Dish("Sweet & sour beef"));
        }
    }

    class SimpleServer {

        private final KitchenService kitchen;

        SimpleServer(KitchenService kitchen) {
            this.kitchen = kitchen;
        }

        Flux<Dish> doingMyJob() {
            return this.kitchen.getDishes() //
                    .map(Dish::deliver);
        }

    }

    static class PoliteServer {

        private final KitchenService kitchen;

        PoliteServer(KitchenService kitchen) {
            this.kitchen = kitchen;
        }

        Flux<Dish> doingMyJob() {
            return this.kitchen.getDishes() //
                    .doOnNext(dish -> System.out.println("Thank you for " + dish + "!"))
                    .doOnError(error -> System.out.println("So sorry about "
                            + error.getMessage()))
                    .doOnComplete(() -> System.out.println("Thanks for all your hard work!"))
                    .map(Dish::deliver);
        }

    }

    class BusyServer {

        private final KitchenService kitchen;

        BusyServer(KitchenService kitchen) {
            this.kitchen = kitchen;
        }

        Flux<Dish> doingMyJob() {
            return this.kitchen.getDishes()
                    .doOnNext(dish -> {
                        System.out.println("Thank you for " + dish + "!");
                        System.out.println("Marking the ticket as done.");
                        System.out.println("Grabbing some silverware.");
                    })
                    .map(this::deliver);
        }

        Dish deliver(Dish dish) {
            return Dish.deliver(dish);
        }
    }

    class BusyServer2 {

        private final KitchenService kitchen;

        BusyServer2(KitchenService kitchen) {
            this.kitchen = kitchen;
        }

        Flux<Dish> doingMyJob() {
            return this.kitchen.getDishes() //
                    .doOnNext(
                            dish -> System.out.println("Thank you for " + dish + "!"))
                    .doOnNext(
                            dish -> System.out.println("Marking the ticket as done."))
                    .doOnNext(dish -> System.out.println("Grabbing some silverware."))
                    .map(this::deliver);
        }

        Dish deliver(Dish dish) {
            return Dish.deliver(dish);
        }
    }

    static class PoliteRestaurant {

        public static void main(String... args) {
            PoliteServer server = new PoliteServer(new KitchenService());

            server.doingMyJob().subscribe(
                    dish -> System.out.println("Consuming " + dish),
                    System.err::println);
        }
    }

}

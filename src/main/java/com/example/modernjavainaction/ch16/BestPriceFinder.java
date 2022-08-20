package com.example.modernjavainaction.ch16;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.example.modernjavainaction.ch16.ExchangeService.*;

public class BestPriceFinder {

    private final List<Shop> shops = Arrays.asList(
            new Shop("BestPrice"),
            new Shop("LetsSaveBig"),
            new Shop("MyFavoriteShop"),
            new Shop("BuyItAll"));

//    public List<String> findPricesSequential(String product) {
//        List<CompletableFuture<String>> priceFutures = shops.stream()
//                .map(shop -> CompletableFuture.supplyAsync(() -> shop.getPrice(product), executor))
//                .map(future -> future.thenApply(Quote::parse))
//                .map(future -> future.thenCompose(quote -> CompletableFuture.supplyAsync(() -> Discount.applyDiscount(quote), executor)))
//                .collect(Collectors.toList());
//
//        return priceFutures
//                .stream()
//                .map(CompletableFuture::join)
//                .collect(Collectors.toList());
//    }

    public List<String> findPricesParallel(String product) {
        return shops.parallelStream()
                .map(shop -> shop.getName() + " price is " + shop.getPrice(product))
                .collect(Collectors.toList());
    }

    private final Executor executor = Executors.newFixedThreadPool(shops.size(), (Runnable r) -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    public List<String> findPricesFuture(String product) {
        List<CompletableFuture<String>> priceFutures =
                shops.stream()
                        .map(shop -> CompletableFuture.supplyAsync(() -> shop.getName() + " price is "
                                + shop.getPrice(product), executor))
                        .collect(Collectors.toList());

        List<String> prices = priceFutures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
        return prices;
    }

    public List<String> findPricesInUSD(String product) {
        List<CompletableFuture<Double>> priceFutures = new ArrayList<>();
        for (Shop shop : shops) {
            // 예제 10-20 시작.
            // 아래 CompletableFuture::join와 호환되도록 futurePriceInUSD의 형식만 CompletableFuture로 바꿈.
            CompletableFuture<Double> futurePriceInUSD =
                    CompletableFuture.supplyAsync(() -> shop.getPrice(product))
                            .thenCombine(
                                    CompletableFuture.supplyAsync(
                                                    () -> getRate(Money.EUR, Money.USD))
                                            // 자바 9에 추가된 타임아웃 관리 기능
                                            .completeOnTimeout(DEFAULT_RATE, 1, TimeUnit.SECONDS),
                                    (price, rate) -> price * rate
                            )
                            // 자바 9에 추가된 타임아웃 관리 기능
                            .orTimeout(3, TimeUnit.SECONDS);
            priceFutures.add(futurePriceInUSD);
        }
        // 단점: 루프 밖에서 shop에 접근할 수 없으므로 아래 getName() 호출을 주석처리함.
        // so the getName() call below has been commented out.
        List<String> prices = priceFutures.stream()
                .map(CompletableFuture::join)
                .map(price -> /*shop.getName() +*/ " price is " + price)
                .collect(Collectors.toList());
        return prices;
    }

}

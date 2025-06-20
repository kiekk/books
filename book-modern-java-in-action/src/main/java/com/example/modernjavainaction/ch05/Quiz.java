package com.example.modernjavainaction.ch05;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Quiz {

    public static void main(String[] args) {
        Trader raoul = new Trader("Raoul", "Cambridge");
        Trader mario = new Trader("Mario", "Milan");
        Trader alan = new Trader("Alan", "Cambridge");
        Trader brian = new Trader("Brian", "Cambridge");

        List<Transaction> transactions = Arrays.asList(
                new Transaction(brian, 2011, 300),
                new Transaction(raoul, 2012, 1000),
                new Transaction(raoul, 2011, 400),
                new Transaction(mario, 2012, 710),
                new Transaction(mario, 2012, 700),
                new Transaction(alan, 2012, 950)
        );

        // 1. 2011년에 일어난 모든 트랜잭션을 찾아 오름차순으로 정리하시오.
        List<Transaction> quiz1 = transactions.stream().filter(transaction -> transaction.getYear() == 2011).sorted(Comparator.comparing(Transaction::getValue)).collect(Collectors.toList());
        // [{Trader:Brian in Cambridge, year: 2011, value: 300}, {Trader:Raoul in Cambridge, year: 2011, value: 400}]
        System.out.println(quiz1);
        System.out.println("---");

        // 2. 거래자가 근무하는 모든 도시를 중복 없이 나열하시오.
        List<String> quiz2 = Stream.of(raoul, mario, alan, brian).map(Trader::getCity).distinct().collect(Collectors.toList());
        // [Cambridge, Milan]
        System.out.println(quiz2);
        System.out.println("---");

        List<String> quiz21 = transactions.stream().map(transaction -> transaction.getTrader().getCity()).distinct().collect(Collectors.toList());
        // [Cambridge, Milan]
        System.out.println(quiz21);
        System.out.println("---");

        Set<String> quiz22 = transactions.stream().map(transaction -> transaction.getTrader().getCity()).collect(Collectors.toSet());
        // [Cambridge, Milan]
        System.out.println(quiz22);
        System.out.println("---");

        // 3. 케임브리지에서 근무하는 모든 거래자를 찾아서 이름순으로 정렬하시오.
        List<String> quiz3 = transactions.stream().filter(transaction -> "Cambridge".equals(transaction.getTrader().getCity())).map(transaction -> transaction.getTrader().getName()).distinct().sorted().collect(Collectors.toList());
        // [Alan, Brian, Raoul]
        System.out.println(quiz3);
        System.out.println("---");

        List<Trader> quiz31 = transactions.stream().map(Transaction::getTrader).filter(trader -> "Cambridge".equals(trader.getCity())).distinct().sorted(Comparator.comparing(Trader::getName)).collect(Collectors.toList());
        // [Trader:Alan in Cambridge, Trader:Brian in Cambridge, Trader:Raoul in Cambridge]
        System.out.println(quiz31);
        System.out.println("---");

        // 4. 모든 거래자의 이름을 알파벳순으로 정렬해서 반환하시오.
        List<String> quiz4 = transactions.stream().map(transaction -> transaction.getTrader().getName()).distinct().sorted().collect(Collectors.toList());
        // [Alan, Brian, Mario, Raoul]
        System.out.println(quiz4);
        System.out.println("---");

        String quiz41 = transactions.stream().map(transaction -> transaction.getTrader().getName()).distinct().sorted().collect(Collectors.joining());
        // AlanBrianMarioRaoul
        System.out.println(quiz41);
        System.out.println("---");

        String quiz42 = transactions.stream().map(transaction -> transaction.getTrader().getName()).distinct().sorted().reduce("", (n1, n2) -> n1 + n2);
        // AlanBrianMarioRaoul
        System.out.println(quiz42);
        System.out.println("---");

        // 5. 밀라노에 거래자가 있는가?
        boolean quiz5 = transactions.stream().anyMatch(transaction -> "Milan".equals(transaction.getTrader().getCity()));
        // true
        System.out.println(quiz5);
        System.out.println("---");

        // 6. 케임브리지에 거주하는 거래자의 모든 트랜잭션값을 출력하시오.
        List<Integer> quiz6 = transactions.stream().filter(transaction -> "Cambridge".equals(transaction.getTrader().getCity())).map(Transaction::getValue).collect(Collectors.toList());
        // [300, 1000, 400, 950]
        System.out.println(quiz6);
        System.out.println("---");

        // 7. 전체 트랜잭션 중 최댓값은 얼마인가?
        Transaction quiz7 = transactions.stream().max(Comparator.comparing(Transaction::getValue)).get();
        // {Trader:Raoul in Cambridge, year: 2012, value: 1000}
        System.out.println(quiz7);

        Integer quiz71 = transactions.stream().map(Transaction::getValue).reduce(0, Integer::max);
        // 1000
        System.out.println(quiz71);
        System.out.println("---");

        // 8. 전체 트랜잭션 중 최솟값은 얼마인가?
        Transaction quiz8 = transactions.stream().min(Comparator.comparing(Transaction::getValue)).get();
        // {Trader:Brian in Cambridge, year: 2011, value: 300}
        System.out.println(quiz8);

        Integer quiz81 = transactions.stream().map(Transaction::getValue).reduce(Integer::min).get();
        // 300
        System.out.println(quiz81);
        System.out.println("---");

    }
}

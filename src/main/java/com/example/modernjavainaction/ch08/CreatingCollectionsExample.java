package com.example.modernjavainaction.ch08;

import static java.util.Map.entry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CreatingCollectionsExample {

    public static void main(String[] args) {
        creatingLists();
        /*
        Arrays.asList 로 컬렉션을 생성하게 되면 AbstractList 를 상속받는데
        AbstractList 의 add, set, remove 는 전부 UnsupportedOperationException 예외를 발생시킵니다.

        따라서 Arrays.asList 로 컬렉션을 생성하면 생성할 때의 컬렉션을 조회하는 것은 가능하지만
        조작하는 것은 불가능합니다.
         */
        /*
        List.of 의 경우 ImmutableCollections.ListN 객체를 호출하게 되는데
        이 객체는 AbstractImmutableList 를 상속받고 있습니다.
        AbstractImmutableList 는 AbstractList 와 비슷하게 add, set, remove 메소드 호출 시 전부 UnsupportedOperationException 예외를 발생시키며,
        추가로 sort 또한 UnsupportedOperationException 예외를 발생시켜 정렬도 불가능한 것을 확인할 수 있습니다.
         */
    }

    private static void creatingLists() {
        System.out.println("------ Creating Lists ------");

        System.out.println("--> Creating a list the old-fashioned way");
        List<String> friends = new ArrayList<>();
        friends.add("Raphael");
        friends.add("Olivia");
        friends.add("Thibaut");
        System.out.println(friends);

        System.out.println("--> Using Arrays.asList()");
        List<String> friends2 = Arrays.asList("Raphael", "Olivia");
        friends2.set(0, "Richard");
        System.out.println(friends2);
        try {
            friends2.add("Thibaut");
            System.out.println("We shouldn't get here...");
        } catch (UnsupportedOperationException e) {
            System.out.println("As expected, we can't add items to a list created with Arrays.asList().");
        }

        System.out.println("--> Creating a Set from a List");
        Set<String> friends3 = new HashSet<>(Arrays.asList("Raphael", "Olivia", "Thibaut"));
        System.out.println(friends3);

        System.out.println("--> Creating a Set from a Stream");
        Set<String> friends4 = Stream.of("Raphael", "Olivia", "Thibaut")
                .collect(Collectors.toSet());
        System.out.println(friends4);

        System.out.println("--> Creating a List with List.of()");
        List<String> friends5 = List.of("Raphael", "Olivia", "Thibaut");
        System.out.println(friends5);
        try {
            friends5.add("Chih-Chun");
            System.out.println("We shouldn't get here...");
        } catch (UnsupportedOperationException e) {
            System.out.println("As expected, we can't add items to a list created with List.of().");
        }
        try {
            friends5.set(1, "Chih-Chun");
            System.out.println("We shouldn't get here...");
        } catch (UnsupportedOperationException e) {
            System.out.println("Neither can we replace items in such a list.");
        }
    }

}

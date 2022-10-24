package chapter09;

import java.util.Arrays;
import java.util.List;

public class MenuTestDrive {
    public static void main(String[] args) {
//        printMenus();

        PancakeHouseMenu pancakeHouseMenu = new PancakeHouseMenu();
        DinerMenu dinerMenu = new DinerMenu();
        printMenusByIterator(pancakeHouseMenu.createIterator());
        printMenusByIterator(dinerMenu.createIterator());
    }

    public static void printMenus() {
        PancakeHouseMenu pancakeHouseMenu = new PancakeHouseMenu();
        DinerMenu dinerMenu = new DinerMenu();

        // 메소드명은 동일, return type 만 다르다.
        List<MenuItem> breakfastItems = pancakeHouseMenu.getMenuItems();
        MenuItem[] lunchItems = dinerMenu.getMenuItems();

        // print with forEach
        System.out.println("=== forEach ===");
        breakfastItems.forEach(System.out::println);
        Arrays.asList(lunchItems).forEach(System.out::println);
        System.out.println("=== forEach ===");

        // Using enhanced for loop
        System.out.println("USING ENHANCED FOR");
        for (MenuItem menuItem : breakfastItems) {
            System.out.print(menuItem.getName());
            System.out.println("\t\t" + menuItem.getPrice());
            System.out.println("\t" + menuItem.getDescription());
        }
        for (MenuItem menuItem : lunchItems) {
            System.out.print(menuItem.getName());
            System.out.println("\t\t" + menuItem.getPrice());
            System.out.println("\t" + menuItem.getDescription());
        }

        // Exposing implementation
        System.out.println("USING FOR LOOPS");
        for (int i = 0; i < breakfastItems.size(); i++) {
            MenuItem menuItem = (MenuItem) breakfastItems.get(i);
            System.out.print(menuItem.getName());
            System.out.println("\t\t" + menuItem.getPrice());
            System.out.println("\t" + menuItem.getDescription());
        }

        for (int i = 0; i < lunchItems.length; i++) {
            MenuItem menuItem = lunchItems[i];
            System.out.print(menuItem.getName());
            System.out.println("\t\t" + menuItem.getPrice());
            System.out.println("\t" + menuItem.getDescription());
        }
    }

    public static void printMenusByIterator(Iterator iterator) {
        while (iterator.hasNext()) {
            MenuItem menuItem = iterator.next();
            System.out.print(menuItem.getName());
            System.out.println("\t\t" + menuItem.getPrice());
            System.out.println("\t" + menuItem.getDescription());
        }
    }
}

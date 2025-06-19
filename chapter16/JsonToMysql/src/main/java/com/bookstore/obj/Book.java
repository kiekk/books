package com.bookstore.obj;

public class Book {
    private String title;
    private String isbn;
    private int price;

    public static Book createBook(String name, String genre, int price) {
        Book book = new Book();
        book.title = name;
        book.isbn = genre;
        book.price = price;
        return book;
    }
}

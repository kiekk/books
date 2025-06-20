package com.bookstore.entity;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@Entity
public class Author {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String genre;
    private int age;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "author_book",
            joinColumns = @JoinColumn(name = "author_id"),
            inverseJoinColumns = @JoinColumn(name = "book_id")
    )
    private Set<Book> books = new HashSet<>();

    protected Author() {
    }

    public Author(Author author, boolean cloneChildren) {
        this.genre = author.getGenre();

        if (!cloneChildren) {
            // associate books
            for (Book book : author.getBooks()) {
                addBook(new Book(book));
            }
//            this.books.addAll(author.getBooks());
        } else {
            // clone each book
            for (Book book : author.getBooks()) {
                addBook(new Book(book));
            }
        }
    }

    public static Author createAuthor(String name, int age, String genre) {
        Author author = new Author();
        author.name = name;
        author.age = age;
        author.genre = genre;

        return author;
    }

    public void addBook(Book book) {
        this.books.add(book);
        book.addAuthor(this);
    }

    public void removeBook(Book book) {
        this.books.remove(book);
        book.removeAuthor(this);
    }

    public void removeBooks() {
        Iterator<Book> iterator = this.books.iterator();

        while (iterator.hasNext()) {
            Book book = iterator.next();

            book.removeAuthor(this);
            iterator.remove();
        }
    }

    public void changeAge(int age) {
        this.age = age;
    }

    public void changeName(String name) {
        this.name = name;
    }

    public String getGenre() {
        return genre;
    }

    public Set<Book> getBooks() {
        return books;
    }

    @Override
    public String toString() {
        return "Author{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", genre='" + genre + '\'' +
                ", age=" + age +
                '}';
    }
}

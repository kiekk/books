package com.bookstore.entity;

import jakarta.persistence.*;

import java.util.*;

@Entity
@Table(name = "author")
public class Author {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String genre;
    private int age;

    // 1 <-> N 단방향 연관관계에서는 mappedBy 속성을 사용할 필요가 없다.
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
//    @JoinColumn(name = "author_id") // @JoinColumn을 사용하면 매핑 테이블을 사용하지 않고도 foreign key를 매핑한다.
    @OrderColumn(name = "books_order") // @OrderColumn은 @OrderColumn + List, @OrderColumn + Set 두 가지 방식을 비교해볼 수 있다.
    private Set<Book> books = new HashSet<>();

    public static Author createAuthor(String name, String genre, int age) {
        Author author = new Author();
        author.name = name;
        author.genre = genre;
        author.age = age;
        return author;
    }

    public void addBooks(Book... books) {
        for (Book book : books) {
            this.books.add(book);
        }
    }

    public void addBook(Book book) {
        this.books.add(book);
    }

    public void removeBook(Book book) {
        this.books.remove(book);
    }

    public void removeBooks() {
        Iterator<Book> iterator = this.books.iterator();
        while (iterator.hasNext()) {
            iterator.remove();
        }
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getGenre() {
        return genre;
    }

    public int getAge() {
        return age;
    }

    public Set<Book> getBooks() {
        return books;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Author author = (Author) o;
        return Objects.equals(id, author.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    // toString() 메서드는 일반적으로 Entity에 있는 필드들로만 출력한다.
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

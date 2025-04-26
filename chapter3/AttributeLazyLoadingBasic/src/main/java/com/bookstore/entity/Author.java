package com.bookstore.entity;

import com.fasterxml.jackson.annotation.JsonFilter;
import jakarta.persistence.*;

@Entity
@JsonFilter("AuthorId")
public class Author {
    @Id
    private Long id;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    private byte[] avatar;

    private int age;
    private String name;
    private String genre;

    public static Author createAuthor(Long id, String name, int age, String genre, byte[] avatar) {
        Author author = new Author();
        author.id = id;
        author.name = name;
        author.age = age;
        author.genre = genre;
        author.avatar = avatar;
        return author;
    }

    // avatar 초기화, 특정 조건에 따른 지연로딩 또는 초기값 설정
    public void initializeAvatar() {
        if (age < 40) {
            getAvatar();
        } else {
            setAvatar(null);
        }
    }

    public void setAvatar(byte[] avatar) {
        this.avatar = avatar;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public byte[] getAvatar() {
        return avatar;
    }

    @Override
    public String toString() {
        return "Author{" +
                "id=" + id +
                ", age=" + age +
                ", genre='" + genre + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}

package com.bookstore.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "author")
public class AuthorDeep extends BaseAuthor {
    @Lob
    private byte[] avatar;

    public static AuthorDeep createAuthorDeep(Long id, int age, String name, String genre, byte[] avatar) {
        AuthorDeep author = new AuthorDeep();
        author.setId(id);
        author.setAge(age);
        author.setName(name);
        author.setGenre(genre);
        author.avatar = avatar;
        return author;
    }

    public byte[] getAvatar() {
        return avatar;
    }
}

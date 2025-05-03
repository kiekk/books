package com.bookstore.entity;

import com.bookstore.dto.AuthorDto;
import jakarta.persistence.*;


@SqlResultSetMappings(
        value = {
                @SqlResultSetMapping(
                        name = "AuthorDtoMapping",
                        classes = @ConstructorResult(
                                targetClass = AuthorDto.class,
                                columns = {
                                        @ColumnResult(name = "name"),
                                        @ColumnResult(name = "age")
                                }
                        )
                ),
                @SqlResultSetMapping(
                        name = "AuthorsNameMapping",
                        columns = {
                                @ColumnResult(name = "name")
                        }
                )
        }
)
@NamedNativeQueries(
        value = {
                @NamedNativeQuery(
                        name = "AuthorDtoQuery",
                        query = "SELECT name, age FROM author",
                        resultSetMapping = "AuthorDtoMapping"
                ),
                @NamedNativeQuery(
                        name = "AuthorsNameQuery",
                        query = "SELECT name FROM author",
                        resultSetMapping = "AuthorsNameMapping"
                )
        }
)
@Entity
public class Author {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int age;
    private String name;
    private String genre;
}

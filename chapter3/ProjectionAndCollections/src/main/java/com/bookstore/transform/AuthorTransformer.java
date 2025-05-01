package com.bookstore.transform;

import com.bookstore.transform.dto.AuthorDto;
import com.bookstore.transform.dto.BookDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AuthorTransformer {
    public List<AuthorDto> transform(List<Object[]> rs) {
        final Map<Long, AuthorDto> authorsDtoMap = new HashMap<>();

        for (Object[] o : rs) {

            Long authorId = ((Number) o[0]).longValue();

            AuthorDto authorDto = authorsDtoMap.get(authorId);
            if (authorDto == null) {
                authorDto = AuthorDto.createAuthorDto((
                                (Number) o[0]).longValue(),
                        (String) o[1],
                        (String) o[2]);
            }

            BookDto bookDto = BookDto.createBookDto(
                    ((Number) o[3]).longValue(),
                    (String) o[4]);

            authorDto.addBook(bookDto);
            authorsDtoMap.putIfAbsent(authorDto.getId(), authorDto);
        }

        return new ArrayList<>(authorsDtoMap.values());
    }
}

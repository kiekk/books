package me.soono.springbootdeveloper.dto.article.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.soono.springbootdeveloper.domain.article.Article;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class AddArticleRequest {

    private String title;
    private String content;

    public Article toEntity() {
        return Article
                .builder()
                .title(title)
                .content(content)
                .build();
    }
}

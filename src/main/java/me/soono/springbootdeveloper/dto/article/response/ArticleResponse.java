package me.soono.springbootdeveloper.dto.article.response;

import me.soono.springbootdeveloper.domain.article.Article;

public record ArticleResponse(String title, String content) {

    public ArticleResponse(Article article) {
        this(article.getTitle(), article.getContent());
    }
}

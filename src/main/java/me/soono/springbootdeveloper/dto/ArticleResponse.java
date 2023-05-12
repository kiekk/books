package me.soono.springbootdeveloper.dto;

import me.soono.springbootdeveloper.domain.Article;

public record ArticleResponse(String title, String content) {

    public ArticleResponse(Article article) {
        this(article.getTitle(), article.getContent());
    }
}

package me.soono.springbootdeveloper.controller;

import lombok.RequiredArgsConstructor;
import me.soono.springbootdeveloper.domain.Article;
import me.soono.springbootdeveloper.dto.AddArticleRequest;
import me.soono.springbootdeveloper.dto.ArticleResponse;
import me.soono.springbootdeveloper.service.BlogService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/articles")
public class BlogApiController {

    private final BlogService blogService;

    @GetMapping("")
    public List<ArticleResponse> findAllArticles() {
        return blogService.findAll()
                .stream()
                .map(ArticleResponse::new)
                .toList();
    }

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    public Article addArticle(@RequestBody AddArticleRequest request) {
        return blogService.save(request);
    }
}

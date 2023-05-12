package me.soono.springbootdeveloper.controller;

import lombok.RequiredArgsConstructor;
import me.soono.springbootdeveloper.domain.Article;
import me.soono.springbootdeveloper.dto.AddArticleRequest;
import me.soono.springbootdeveloper.service.BlogService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/articles")
public class BlogApiController {

    private final BlogService blogService;

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    public Article addArticle(@RequestBody AddArticleRequest request) {
        return blogService.save(request);
    }
}

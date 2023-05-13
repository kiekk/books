package me.soono.springbootdeveloper.controller.blog;

import lombok.RequiredArgsConstructor;
import me.soono.springbootdeveloper.domain.article.Article;
import me.soono.springbootdeveloper.dto.article.request.AddArticleRequest;
import me.soono.springbootdeveloper.dto.article.response.ArticleResponse;
import me.soono.springbootdeveloper.dto.article.request.UpdateArticleRequest;
import me.soono.springbootdeveloper.service.blog.BlogService;
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

    @GetMapping("{id}")
    public ArticleResponse findArticle(@PathVariable long id) {
        return new ArticleResponse(blogService.findById(id));
    }

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    public Article addArticle(@RequestBody AddArticleRequest request) {
        return blogService.save(request);
    }

    @PutMapping("{id}")
    public Article updateArticle(@PathVariable long id,
                                @RequestBody UpdateArticleRequest request) {
        return blogService.update(id, request);
    }

    @DeleteMapping("{id}")
    public void deleteArticle(@PathVariable long id) {
        blogService.delete(id);
    }

}

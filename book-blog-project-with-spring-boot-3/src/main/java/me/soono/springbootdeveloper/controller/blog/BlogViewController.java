package me.soono.springbootdeveloper.controller.blog;

import lombok.RequiredArgsConstructor;
import me.soono.springbootdeveloper.domain.article.Article;
import me.soono.springbootdeveloper.dto.article.response.ArticleListViewResponse;
import me.soono.springbootdeveloper.dto.article.response.ArticleViewResponse;
import me.soono.springbootdeveloper.service.blog.BlogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class BlogViewController {

    private final BlogService blogService;

    @GetMapping("articles")
    public String getArticles(Model model) {
        List<ArticleListViewResponse> articles = blogService.findAll()
                .stream()
                .map(ArticleListViewResponse::new)
                .toList();

        model.addAttribute("articles", articles);
        return "articleList";
    }

    @GetMapping("articles/{id}")
    public String getArticle(@PathVariable Long id, Model model) {
        model.addAttribute("article", new ArticleViewResponse(blogService.findById(id)));
        return "article";
    }

    @GetMapping("new-article")
    public String newArticle(@RequestParam(required = false) Long id, Model model) {
        if (id == null) {
            model.addAttribute("article", new ArticleViewResponse());
        } else {
            Article article = blogService.findById(id);
            model.addAttribute("article", new ArticleViewResponse(article));
        }
        return "newArticle";
    }

}

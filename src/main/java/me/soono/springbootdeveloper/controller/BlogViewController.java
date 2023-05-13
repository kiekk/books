package me.soono.springbootdeveloper.controller;

import lombok.RequiredArgsConstructor;
import me.soono.springbootdeveloper.dto.ArticleListViewResponse;
import me.soono.springbootdeveloper.service.BlogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("articles")
public class BlogViewController {

    private final BlogService blogService;

    @GetMapping("")
    public String getArticles(Model model) {
        List<ArticleListViewResponse> articles = blogService.findAll()
                .stream()
                .map(ArticleListViewResponse::new)
                .toList();

        model.addAttribute("articles", articles);
        return "articleList";
    }

}

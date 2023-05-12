package me.soono.springbootdeveloper.service;

import lombok.RequiredArgsConstructor;
import me.soono.springbootdeveloper.domain.Article;
import me.soono.springbootdeveloper.dto.AddArticleRequest;
import me.soono.springbootdeveloper.repository.BlogRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BlogService {

    private final BlogRepository blogRepository;

    public Article save(AddArticleRequest request) {
        return blogRepository.save(request.toEntity());
    }
}

package me.soono.springbootdeveloper.repository.blog;

import me.soono.springbootdeveloper.domain.article.Article;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlogRepository extends JpaRepository<Article, Long> {
}

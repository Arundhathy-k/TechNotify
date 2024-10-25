package com.kovan.mapper;

import com.kovan.dto.NewsDto;
import com.kovan.entity.NewsEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class NewsMapper {

    public NewsDto toDto(NewsEntity newsEntity) {
        if (Objects.isNull(newsEntity)) {
            return null;
        }
        return NewsDto.builder()
                .id(newsEntity.getId())
                .status(newsEntity.getStatus())
                .totalResults(newsEntity.getTotalResults())
                .publishedAt(newsEntity.getPublishedAt() != null ? newsEntity.getPublishedAt() : null)
                .articles(mapArticlesToDto(newsEntity.getArticles()))
                .build();
    }

    public NewsEntity toEntity(NewsDto newsDto) {
        if (Objects.isNull(newsDto)) {
            return null;
        }
        return NewsEntity.builder()
                .id(newsDto.getId())
                .status(newsDto.getStatus())
                .totalResults(newsDto.getTotalResults())
                .publishedAt(newsDto.getPublishedAt() != null ? newsDto.getPublishedAt() : null)
                .articles(mapArticlesToEntity(newsDto.getArticles()))
                .build();
    }

    private List<NewsDto.Article> mapArticlesToDto(List<NewsEntity.Article> articles) {
        return articles != null ? articles.stream()
                .map(article -> NewsDto.Article.builder()
                        .source(mapSourceToDto(article.getSource()))
                        .author(article.getAuthor())
                        .title(article.getTitle())
                        .description(article.getDescription())
                        .url(article.getUrl())
                        .urlToImage(article.getUrlToImage())
                        .publishedAt(article.getPublishedAt())
                        .content(article.getContent())
                        .build())
                .collect(Collectors.toList()) : null;
    }


    private List<NewsEntity.Article> mapArticlesToEntity(List<NewsDto.Article> articles) {
        return articles != null ? articles.stream()
                .map(article -> NewsEntity.Article.builder()
                        .source(mapSourceToEntity(article.getSource()))
                        .author(article.getAuthor())
                        .title(article.getTitle())
                        .description(article.getDescription())
                        .url(article.getUrl())
                        .urlToImage(article.getUrlToImage())
                        .publishedAt(article.getPublishedAt())
                        .content(article.getContent())
                        .build())
                .collect(Collectors.toList()) : null;
    }


    private NewsDto.Article.Source mapSourceToDto(NewsEntity.Article.Source source) {
        return source != null ? NewsDto.Article.Source.builder()
                .id(source.getId())
                .name(source.getName())
                .build() : null;
    }

    private NewsEntity.Article.Source mapSourceToEntity(NewsDto.Article.Source source) {
        return source != null ? NewsEntity.Article.Source.builder()
                .id(source.getId())
                .name(source.getName())
                .build() : null;
    }
}

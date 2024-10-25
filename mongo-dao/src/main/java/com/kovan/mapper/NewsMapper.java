package com.kovan.mapper;

import com.kovan.dto.NewsDto;
import com.kovan.entity.NewsEntity;
import io.micrometer.common.util.StringUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
                .publishedAt(StringUtils.isNotEmpty(newsEntity.getPublishedAt()) ? newsEntity.getPublishedAt() : null)
                .articles(mapArticlesToDto(newsEntity.getArticles()))
                .build();
    }

    public NewsEntity toEntity(NewsDto newsDto,String createdby,String updatedby) {
        if (Objects.isNull(newsDto)) {
            return null;
        }
        return NewsEntity.builder()
                .id(newsDto.getId())
                .status(newsDto.getStatus())
                .totalResults(newsDto.getTotalResults())
                .publishedAt(StringUtils.isNotEmpty(newsDto.getPublishedAt()) ? newsDto.getPublishedAt() : null)
                .articles(mapArticlesToEntity(newsDto.getArticles()))
                .createdBy(createdby)
                .updatedBy(updatedby)
                .build();
    }

    private List<NewsDto.Article> mapArticlesToDto(List<NewsEntity.Article> articles) {
        return Optional.ofNullable(articles)
                .map(list -> list.stream()
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
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }


    private List<NewsEntity.Article> mapArticlesToEntity(List<NewsDto.Article> articles) {
        return Optional.ofNullable(articles)
                .map(list -> list.stream()
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
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }


    private NewsDto.Article.Source mapSourceToDto(NewsEntity.Article.Source source) {
        return Optional.ofNullable(source)
                .map(src -> NewsDto.Article.Source.builder()
                        .id(src.getId())
                        .name(src.getName())
                        .build())
                .orElse(null);
    }

    private NewsEntity.Article.Source mapSourceToEntity(NewsDto.Article.Source source) {
        return Optional.ofNullable(source)
                .map(src -> NewsEntity.Article.Source.builder()
                        .id(src.getId())
                        .name(src.getName())
                        .build())
                .orElse(null);
    }
}

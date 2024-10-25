package com.kovan.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kovan.entity.NewsEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NewsDto {

    @Id
    private String id;

    private String status;

    private int totalResults;

    private String publishedAt;

    private   List<Article> articles;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Article {
        private Source source;
        private String author;
        private String title;
        private String description;
        private String url;
        private String urlToImage;
        private String publishedAt;
        private String content;

        @Data
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Source {
          private String id;
          private String name;


        }
    }
}


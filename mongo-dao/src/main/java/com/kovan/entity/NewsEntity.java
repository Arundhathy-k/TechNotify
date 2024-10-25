package com.kovan.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "News")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NewsEntity {

    @Id
    private String id;

    private String status;

    private int totalResults;

    private String publishedAt;

    private List<Article> articles;

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

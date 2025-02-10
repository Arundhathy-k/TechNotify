package com.kovan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NewsDto  implements Serializable {

    private String id;

    private String status;

    private int totalResults;

    private String publishedAt;

    private List<Article> articles;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Article implements Serializable{
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
        public static class Source implements Serializable{
            private String id;
            private String name;
        }
    }
}
package com.kovan.app.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kovan.exception.NewsRetrievalException;
import com.kovan.dto.NewsDto;
import com.kovan.repository.NewsRepository;
import com.kovan.service.NewsRepositoryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


@Service
public class NewsService {

    @Value("${news.api.url}")
    private String apiUrl;

    @Value("${news.api.country}")
    private String country;

    @Value("${news.api.category}")
    private String category;

    @Value("${news.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final NewsRepositoryService service;
    private final NewsRepository newsRepository;

    public NewsService(RestTemplate restTemplate, ObjectMapper objectMapper,
                       NewsRepositoryService service, NewsRepository newsRepository) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.service = service;
        this.newsRepository = newsRepository;
    }

    public NewsDto getTopHeadlines() {

        List<NewsDto.Article> articles = new ArrayList<>();
        int pageSize = 0;
        int page = 1;
        NewsDto newsDto = new NewsDto();

        do {
            pageSize += 20;
            String apiUrl = buildUrl(page++);
            String response;
            try {
                response = restTemplate.getForObject(apiUrl, String.class);
                newsDto = objectMapper.readValue(response, NewsDto.class);
                articles.addAll(newsDto.getArticles());
            } catch (JsonProcessingException e) {
                throw new NewsRetrievalException("Failed to parse news data from API response.", e);
            }

        } while (pageSize <= newsDto.getTotalResults());

        LocalDate dateOnly = LocalDate.parse(
                articles.getFirst().getPublishedAt(),
                DateTimeFormatter.ISO_DATE_TIME
        );

        if (!isNewsAlreadyInDb(dateOnly.toString())) {
            NewsDto finalDto = NewsDto.builder()
                    .articles(articles)
                    .totalResults(articles.size())
                    .publishedAt(dateOnly.toString())
                    .build();

            newsDto = service.saveNewsInDb(finalDto);
        }
        return newsDto;
    }

    private boolean isNewsAlreadyInDb(String publishedAt) {
        return newsRepository.findByPublishedAt(publishedAt).isPresent();
    }

    private String buildUrl(int page) {
        return String.format("%s?country=%s&category=%s&page=%d&apiKey=%s",
                apiUrl, country, category, page, apiKey);
    }

    public List<NewsDto> getAllData() {
        return service.getAllNewsFromDb();
    }

}

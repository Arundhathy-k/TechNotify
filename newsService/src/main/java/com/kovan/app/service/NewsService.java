package com.kovan.app.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kovan.dto.NewsDto;
import com.kovan.entity.NewsEntity;
import com.kovan.exception.NewsRetrievalException;
import com.kovan.mapper.NewsMapper;
import com.kovan.repository.NewsRepository;
import com.kovan.service.NewsRepositoryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import static java.util.stream.Collectors.*;
import static java.util.Objects.*;

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
    private final NewsMapper newsMapper;

    LocalDate today = LocalDate.now();
    LocalDate yesterday = today.minusDays(1);

    public NewsService(RestTemplate restTemplate, ObjectMapper objectMapper,
                       NewsRepositoryService service, NewsRepository newsRepository, NewsMapper newsMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.service = service;
        this.newsRepository = newsRepository;
        this.newsMapper = newsMapper;
    }

    public List<NewsDto> getTopHeadlines() {

        List<NewsDto.Article> articles = new ArrayList<>();
        int pageSize = 0;
        int page = 1;
        NewsDto newsDto;

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

        boolean isYesterdayInDb = isNewsAlreadyInDb(yesterday.toString());
        boolean isTodayInDb = isNewsAlreadyInDb(today.toString());

        Map<Boolean, List<NewsDto.Article>> partitionedArticles = articles.stream()
                .takeWhile(article -> !parseDate(article.getPublishedAt()).isBefore(yesterday))
                .collect(partitioningBy(article -> parseDate(article.getPublishedAt()).equals(yesterday)));

        List<NewsDto.Article> newsArticles = partitionedArticles.get(true);
        List<NewsDto.Article> futureArticles = partitionedArticles.get(false);

        if (newsArticles.isEmpty() && futureArticles.isEmpty() && !isYesterdayInDb) {
            NewsDto emptyNews = NewsDto.builder().totalResults(0)
                    .publishedAt(yesterday.toString()).status("fail").build();
            service.saveNewsInDb(emptyNews);
        } else {
            if (!isYesterdayInDb && !newsArticles.isEmpty()) {
                saveNews(newsArticles, yesterday);
            }
            if (!isTodayInDb && !futureArticles.isEmpty()) {
                saveNews(futureArticles, today);
            }
        }
         return fetchSavedNews(futureArticles);
    }
    private LocalDate parseDate(String publishedAt) {
        return LocalDate.parse(publishedAt, DateTimeFormatter.ISO_DATE_TIME);
    }

    private void saveNews(List<NewsDto.Article> articles, LocalDate date) {
        NewsDto finalDto = NewsDto.builder()
                .articles(articles)
                .status("ok")
                .totalResults(articles.size())
                .publishedAt(date.toString())
                .build();
        service.saveNewsInDb(finalDto);
    }
    private List<NewsDto> fetchSavedNews(List<NewsDto.Article> futureArticles) {
        List<String> datesToFetch = new ArrayList<>();
        datesToFetch.add(yesterday.toString());
        if (nonNull(futureArticles)) {
            datesToFetch.add(today.toString());
        }
        List<NewsEntity> newsEntities = newsRepository.findByPublishedAtIn(datesToFetch);

        return newsEntities.stream().map(newsMapper::toDto).collect(toList());
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

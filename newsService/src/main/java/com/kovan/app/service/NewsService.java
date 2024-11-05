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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    LocalDate currentDate = LocalDate.now();
    LocalDate yesterday = currentDate.minusDays(1);

    public NewsService(RestTemplate restTemplate, ObjectMapper objectMapper,
                       NewsRepositoryService service, NewsRepository newsRepository, NewsMapper newsMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.service = service;
        this.newsRepository = newsRepository;
        this.newsMapper = newsMapper;
    }

    public NewsDto getTopHeadlines() {

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

        List<NewsDto.Article> newArticles = null;
        List<NewsDto.Article> latestArticles = null;

        boolean isYesterdayInDb = isNewsAlreadyInDb(yesterday.toString());
        boolean isTodayInDb = isNewsAlreadyInDb(currentDate.toString());

       for(NewsDto.Article article: articles){
            LocalDate date = LocalDate.parse(article.getPublishedAt(), DateTimeFormatter.ISO_DATE_TIME);
            if (date.equals(yesterday)) {
                if (isNull(newArticles)) {
                    newArticles = new ArrayList<>();
                }
                newArticles.add(article);
            } else if (date.isBefore(yesterday)) {
               break;
            } else if (date.isAfter(yesterday)) {
                if (isNull(latestArticles)) {
                    latestArticles = new ArrayList<>();
                }
                latestArticles.add(article);
            }
        }
        if (isNull(newArticles) && isNull(latestArticles) && !isYesterdayInDb) {
            NewsDto emptyNews = NewsDto.builder().totalResults(0)
                    .publishedAt(yesterday.toString()).status("fail").build();
            service.saveNewsInDb(emptyNews);
        } else {
            if (!isYesterdayInDb && nonNull(newArticles)) {
                saveNews(newArticles, yesterday);
            }
            if (!isTodayInDb && nonNull(latestArticles)) {
                saveNews(latestArticles, currentDate);
            }
        }
        return Optional.ofNullable(newsRepository.findByPublishedAt(yesterday.toString()))
                .map(newsMapper::toDto)
                .orElse(null);

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
    private boolean isNewsAlreadyInDb(String publishedAt) {
        NewsEntity entity = newsRepository.findByPublishedAt(publishedAt);
        return nonNull(entity);
    }

    private String buildUrl(int page) {
        return String.format("%s?country=%s&category=%s&page=%d&apiKey=%s",
                apiUrl, country, category, page, apiKey);
    }

    public List<NewsDto> getAllData() {
        return service.getAllNewsFromDb();
    }
}

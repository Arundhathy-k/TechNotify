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
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
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

    System.Logger logger = System.getLogger("DateLogger");

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

        AtomicReference<List<NewsDto.Article>> newsArticles = new AtomicReference<>();
        AtomicReference<List<NewsDto.Article>> futureArticles = new AtomicReference<>();

        boolean isYesterdayInDb = isNewsAlreadyInDb(yesterday.toString());
        boolean isTodayInDb = isNewsAlreadyInDb(today.toString());

        articles.stream()
                .takeWhile(article -> {
                    LocalDate date = LocalDate.parse(article.getPublishedAt(), DateTimeFormatter.ISO_DATE_TIME);
                    logger.log(System.Logger.Level.INFO, "parsedDate = {0}", date);
                    return !date.isBefore(yesterday);
                })
                .forEach(article -> {
                    LocalDate date = LocalDate.parse(article.getPublishedAt(), DateTimeFormatter.ISO_DATE_TIME);
                    if (date.equals(yesterday)) {
                        logger.log(System.Logger.Level.INFO, "equal = {0}", date);
                        newsArticles.set(createList(newsArticles.get()));
                        newsArticles.get().add(article);
                    } else {
                        logger.log(System.Logger.Level.INFO, "isAfter = {0}", date);
                        futureArticles.set(createList(futureArticles.get()));
                        futureArticles.get().add(article);
                    }
                });


        if (isNull(newsArticles.get()) && isNull(futureArticles.get()) && !isYesterdayInDb) {
            NewsDto emptyNews = NewsDto.builder().totalResults(0)
                    .publishedAt(yesterday.toString()).status("fail").build();
            service.saveNewsInDb(emptyNews);
        } else {
            if (!isYesterdayInDb && nonNull(newsArticles.get())) {
                saveNews(newsArticles.get(), yesterday);
            }
            if (!isTodayInDb && nonNull(futureArticles.get())) {
                saveNews(futureArticles.get(), today);
            }
        }
         return fetchSavedNews();
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
    private List<NewsDto.Article> createList(List<NewsDto.Article> list) {
        return (isNull(list)) ? new ArrayList<>() : list;
    }
    private List<NewsDto> fetchSavedNews() {
        String todayDate = today.toString();
        String yesterdayDate = yesterday.toString();

        List<NewsEntity> newsEntities = new ArrayList<>();

        Optional<NewsEntity> todayNews = newsRepository.findByPublishedAt(todayDate);

        if (todayNews.isPresent()) {
            newsEntities.add(todayNews.get());
            newsRepository.findByPublishedAt(yesterdayDate).ifPresent(newsEntities::add);
        } else {
            newsRepository.findByPublishedAt(yesterdayDate).ifPresent(newsEntities::add);
        }

        return newsEntities.stream().map(newsMapper::toDto).collect(Collectors.toList());
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

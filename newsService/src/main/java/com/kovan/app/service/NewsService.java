package com.kovan.app.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kovan.dto.NewsDto;
import com.kovan.exception.NewsRetrievalException;
import com.kovan.service.NewsRepositoryService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static java.time.LocalDate.now;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.*;
import static java.util.Objects.*;
import static java.util.stream.Stream.iterate;

@Service
@Slf4j
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
    private final NewsRepositoryService newsRepositoryService;

    private static final Logger logger = LoggerFactory.getLogger(NewsService.class);
    private static final int DEFAULT_PAGE_SIZE = 20;

    LocalDate today = now();
    LocalDate yesterday = today.minusDays(1);

    public NewsService(RestTemplate restTemplate, ObjectMapper objectMapper,
                       NewsRepositoryService newsRepositoryService) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.newsRepositoryService = newsRepositoryService;
    }

    @Scheduled(cron = "0 0 */8 * * *")
    public List<NewsDto> getTopHeadlines() {

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS");

        String strDate = dateFormat.format(new Date());

        logger.info("Task running at - {}", strDate);

        List<NewsDto.Article> newsArticles;
        List<NewsDto.Article> futureArticles;
        Optional<NewsDto> savedYesterdayNewsDto;
        Optional<NewsDto> savedTodayNewsDto;

        AtomicInteger pageSize = new AtomicInteger(0);

        AtomicReference<List<NewsDto.Article>> articles= new AtomicReference<>();

        iterate(1, page -> page + 1)
                .map(page -> {
                    String url = buildUrl(page);
                    String response;
                    try {
                        response = restTemplate.getForObject(url, String.class);
                        NewsDto newsDto =  objectMapper.readValue(response, NewsDto.class);
                        if(isNull(articles.get())) {
                            articles.set(new ArrayList<>());
                        }
                        articles.get().addAll(newsDto.getArticles());
                        return newsDto;
                    } catch (JsonProcessingException e) {
                        throw new NewsRetrievalException("Failed to parse news data from API response.", e);
                    }
                })
                .takeWhile(newsDto -> pageSize.addAndGet(DEFAULT_PAGE_SIZE) < newsDto.getTotalResults())
                .toList();

        boolean isYesterdayInDb = isNewsAlreadyInDb(yesterday.toString());
        boolean isTodayInDb = isNewsAlreadyInDb(today.toString());

        Map<Boolean, List<NewsDto.Article>> partitionedArticles = articles.get().stream()
                .takeWhile(article -> !parseDate(article.getPublishedAt()).isBefore(yesterday))
                .collect(partitioningBy(article -> parseDate(article.getPublishedAt()).equals(yesterday)));

        newsArticles = partitionedArticles.get(true);
        futureArticles = partitionedArticles.get(false);

        if (newsArticles.isEmpty() && futureArticles.isEmpty() && !isYesterdayInDb) {
            NewsDto emptyNews = NewsDto.builder().totalResults(0)
                    .publishedAt(yesterday.toString()).status("fail").build();
           return singletonList(newsRepositoryService.saveNewsInDb(emptyNews));
        } else {
            savedYesterdayNewsDto = handleNewsSaving(isYesterdayInDb,newsArticles,yesterday);
            savedTodayNewsDto = handleNewsSaving(isTodayInDb,futureArticles,today);
        }

       return List.of(savedTodayNewsDto.get(),savedYesterdayNewsDto.get());
    }

    private Optional<NewsDto> handleNewsSaving(boolean isInDb, List<NewsDto.Article> articles, LocalDate date) {

        Optional<NewsDto> existingNews = fetchNewsFromDatabase(isInDb, date);

        return existingNews
                .map(news -> {
                    if (articles.size() > news.getTotalResults()) {
                        return updateNews(articles, date).get();
                    }
                    return news;
                })
                .or(() -> saveNews(articles, date));
    }

    private Optional<NewsDto> fetchNewsFromDatabase(boolean isInDb, LocalDate date) {
        if (isInDb) {
            return newsRepositoryService.findNewsInDb(date.toString());
        }
        return empty();
    }

    private LocalDate parseDate(String publishedAt) {
        return LocalDate.parse(publishedAt, DateTimeFormatter.ISO_DATE_TIME);
    }
    private Optional<NewsDto> saveNews(List<NewsDto.Article> articles, LocalDate date) {
        NewsDto finalDto = NewsDto.builder()
                .articles(articles)
                .status("ok")
                .totalResults(articles.size())
                .publishedAt(date.toString())
                .build();
        NewsDto savedNews = newsRepositoryService.saveNewsInDb(finalDto);
        if(isNull(savedNews)){
            return Optional.empty();
        }
        return of(savedNews);
    }
    private Optional<NewsDto> updateNews(List<NewsDto.Article> articles, LocalDate date) {
        NewsDto finalDto = NewsDto.builder()
                .articles(articles)
                .status("ok")
                .totalResults(articles.size())
                .publishedAt(date.toString())
                .build();

        return newsRepositoryService.updateNewsInDb(date.toString(),finalDto);
    }
    private boolean isNewsAlreadyInDb(String publishedAt) {
        return newsRepositoryService.findNewsInDb(publishedAt).isPresent();
    }

    private String buildUrl(int page) {
        return String.format("%s?country=%s&category=%s&page=%d&apiKey=%s",
                apiUrl, country, category, page, apiKey);
    }

    public List<NewsDto> getAllData() {
        return newsRepositoryService.getAllNewsFromDb();
    }
}

package com.kovan.app.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kovan.dto.NewsDto;
import com.kovan.exception.NewsRetrievalException;
import com.kovan.repository.NewsRepository;
import com.kovan.service.NewsRepositoryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static java.util.stream.Stream.iterate;
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

    private static final int DEFAULT_PAGE_SIZE = 20;

    LocalDate today = LocalDate.now();
    LocalDate yesterday = today.minusDays(1);

    public NewsService(RestTemplate restTemplate, ObjectMapper objectMapper,
                       NewsRepositoryService service, NewsRepository newsRepository) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.service = service;
        this.newsRepository = newsRepository;
    }
    public List<NewsDto> getTopHeadlines() {

        AtomicInteger PAGE_SIZE = new AtomicInteger(0);

        AtomicReference<List<NewsDto.Article>> articles= new AtomicReference<>();

        iterate(1, page -> page + 1)
                .map(page -> {
                    String apiUrl = buildUrl(page);
                    String response;
                    try {
                        response = restTemplate.getForObject(apiUrl, String.class);
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
                .takeWhile(newsDto -> PAGE_SIZE.addAndGet(DEFAULT_PAGE_SIZE) < newsDto.getTotalResults())
                .toList();

        boolean isYesterdayInDb = isNewsAlreadyInDb(yesterday.toString());
        boolean isTodayInDb = isNewsAlreadyInDb(today.toString());

        Map<Boolean, List<NewsDto.Article>> partitionedArticles = articles.get().stream()
                .takeWhile(article -> !parseDate(article.getPublishedAt()).isBefore(yesterday))
                .collect(partitioningBy(article -> parseDate(article.getPublishedAt()).equals(yesterday)));

        List<NewsDto.Article> newsArticles = partitionedArticles.get(true);
        List<NewsDto.Article> futureArticles = partitionedArticles.get(false);
        Optional <NewsDto> savedYesterdayNewsDto;
        Optional <NewsDto> savedTodayNewsDto;

        if (newsArticles.isEmpty() && futureArticles.isEmpty() && !isYesterdayInDb) {
            NewsDto emptyNews = NewsDto.builder().totalResults(0)
                    .publishedAt(yesterday.toString()).status("fail").build();
           return Collections.singletonList(service.saveNewsInDb(emptyNews));
        } else {
            savedYesterdayNewsDto = handleNewsSaving(isYesterdayInDb,newsArticles,yesterday);
            savedTodayNewsDto = handleNewsSaving(isTodayInDb,futureArticles,today);
        }

       return Stream.of(savedTodayNewsDto, savedYesterdayNewsDto)
               .flatMap(Optional::stream)
               .toList();
    }

    private Optional<NewsDto> handleNewsSaving(boolean isInDb, List<NewsDto.Article> articles,LocalDate date) {
        if(!isInDb && !articles.isEmpty()){
           return Optional.of(saveNews(articles,date));
        }
        return Optional.empty();
    }
    private LocalDate parseDate(String publishedAt) {
        return LocalDate.parse(publishedAt, DateTimeFormatter.ISO_DATE_TIME);
    }
    private NewsDto saveNews(List<NewsDto.Article> articles, LocalDate date) {
        NewsDto finalDto = NewsDto.builder()
                .articles(articles)
                .status("ok")
                .totalResults(articles.size())
                .publishedAt(date.toString())
                .build();
       return service.saveNewsInDb(finalDto);
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

package com.kovan;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kovan.app.service.NewsService;
import com.kovan.dto.NewsDto;
import com.kovan.exception.NewsRetrievalException;
import com.kovan.service.NewsRepositoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import static java.util.Collections.*;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewsServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private NewsRepositoryService service;

    @InjectMocks
    private NewsService newsService;

    LocalDate today = LocalDate.now();
    LocalDate yesterday = today.minusDays(1);

    LocalDateTime todayWithTime = LocalDateTime.now();
    LocalDateTime yesterdayWithTime = todayWithTime.minusDays(1);

    @Test
    void testGetTopHeadlines_Success() throws Exception {

        String apiResponse = "{ \"articles\": ["
                + "{ \"title\": \"Today's News\", \"publishedAt\": \""+ todayWithTime + "\" },"
                + "{ \"title\": \"Yesterday's News\", \"publishedAt\": \""+ yesterdayWithTime + "\" }"
                + "], \"totalResults\": 2 }";

        NewsDto.Article todaysArticle = NewsDto.Article.builder()
                .title("Today's News")
                .publishedAt(todayWithTime.toString())
                .build();

        NewsDto.Article yesterdaysArticle = NewsDto.Article.builder()
                .title("Yesterday's News")
                .publishedAt(yesterdayWithTime.toString())
                .build();

        NewsDto apiNewsDto = NewsDto.builder()
                .totalResults(2)
                .articles(List.of(todaysArticle, yesterdaysArticle))
                .build();

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(apiResponse);
        when(objectMapper.readValue(anyString(), eq(NewsDto.class))).thenReturn(apiNewsDto);
        when(service.findNewsInDb(yesterday.toString())).thenReturn(Optional.empty());
        when(service.findNewsInDb(today.toString())).thenReturn(Optional.empty());
        when(service.saveNewsInDb(any(NewsDto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<NewsDto> result = newsService.getTopHeadlines();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(today.toString(), result.get(0).getPublishedAt());
        assertEquals(yesterday.toString(), result.get(1).getPublishedAt());

        verify(service).saveNewsInDb(argThat(news -> news.getPublishedAt().equals(yesterday.toString())));
        verify(service).saveNewsInDb(argThat(news -> news.getPublishedAt().equals(today.toString())));
    }

    @Test
    void testGetTopHeadlines_NoArticlesFetched() throws Exception {
        String apiResponse = "{ \"articles\": [], \"totalResults\": 0 }";

        NewsDto emptyApiNewsDto = NewsDto.builder()
                .totalResults(0)
                .articles(emptyList())
                .build();

        NewsDto expectedEmptyNews = NewsDto.builder()
                .totalResults(0)
                .status("fail")
                .publishedAt(yesterday.toString())
                .build();

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(apiResponse);
        when(objectMapper.readValue(anyString(), eq(NewsDto.class))).thenReturn(emptyApiNewsDto);
        when(service.findNewsInDb(yesterday.toString())).thenReturn(Optional.empty());
        when(service.saveNewsInDb(any(NewsDto.class))).thenReturn(expectedEmptyNews);

        List<NewsDto> result = newsService.getTopHeadlines();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("fail", result.getFirst().getStatus());
        assertEquals(yesterday.toString(), result.getFirst().getPublishedAt());
        assertEquals(0, result.getFirst().getTotalResults());

        verify(service).saveNewsInDb(argThat(news ->
                news.getPublishedAt().equals(yesterday.toString()) &&
                        news.getStatus().equals("fail") &&
                        news.getTotalResults() == 0
        ));
    }

    @Test
    void testGetTopHeadlines_OnlyYesterdayNewsPresentInApi() throws Exception {

        String apiResponse = "{ \"articles\": [" +
                "{ \"title\": \"Yesterday's News\", \"publishedAt\": \"" + yesterdayWithTime + "\"}" +
                "], \"totalResults\": 1 }";

        NewsDto.Article yesterdaysArticle = NewsDto.Article.builder()
                .title("Yesterday's News")
                .publishedAt(yesterdayWithTime.toString())
                .build();

        NewsDto apiNewsDto = NewsDto.builder()
                .totalResults(1)
                .articles(List.of(yesterdaysArticle))
                .build();

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(apiResponse);
        when(objectMapper.readValue(anyString(), eq(NewsDto.class))).thenReturn(apiNewsDto);
        when(service.findNewsInDb(yesterday.toString())).thenReturn(Optional.empty());
        when(service.findNewsInDb(today.toString())).thenReturn(Optional.empty());

        when(service.saveNewsInDb(any(NewsDto.class))).thenReturn(
                NewsDto.builder().publishedAt(yesterday.toString()).status("ok").totalResults(1).build());

        List<NewsDto> result = newsService.getTopHeadlines();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(yesterday.toString(), result.getFirst().getPublishedAt());
        assertEquals(1, result.getFirst().getTotalResults());
        assertEquals("ok", result.getFirst().getStatus());

        verify(service).saveNewsInDb(argThat(news ->
                news.getPublishedAt().equals(yesterday.toString()) &&
                        news.getTotalResults() == 1));
    }

    @Test
    void testUpdateNewsInDb_ShouldUpdateExistingNews() throws Exception {
        String publishedAtDate = yesterday.toString();

        NewsDto existingNewsDto = NewsDto.builder()
                .publishedAt(publishedAtDate)
                .totalResults(1)
                .articles(List.of(
                        NewsDto.Article.builder()
                                .title("Old Article")
                                .publishedAt(publishedAtDate)
                                .build()
                ))
                .status("ok")
                .build();

        List<NewsDto.Article> updatedArticles = List.of(
                NewsDto.Article.builder()
                        .title("Updated Article 1")
                        .publishedAt(yesterdayWithTime.toString())
                        .build(),
                NewsDto.Article.builder()
                        .title("Updated Article 2")
                        .publishedAt(yesterdayWithTime.toString())
                        .build()
        );

        List<NewsDto.Article> articles = List.of(
                NewsDto.Article.builder()
                        .title("Updated Article 1")
                        .publishedAt(publishedAtDate)
                        .build(),
                NewsDto.Article.builder()
                        .title("Updated Article 2")
                        .publishedAt(publishedAtDate)
                        .build()
        );

        NewsDto updatedNewsDto = NewsDto.builder()
                .publishedAt(publishedAtDate)
                .totalResults(articles.size())
                .articles(articles)
                .status("ok")
                .build();

        String apiResponse = "{ \"articles\": [" +
                "{ \"title\": \"Updated Article 1\", \"publishedAt\": \"" + yesterdayWithTime + "\"}," +
                "{ \"title\": \"Updated Article 2\", \"publishedAt\": \"" + yesterdayWithTime + "\"}" +
                "], \"totalResults\": 2 }";

        NewsDto apiNewsDto = NewsDto.builder()
                .totalResults(2)
                .articles(updatedArticles)
                .build();

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(apiResponse);
        when(objectMapper.readValue(anyString(), eq(NewsDto.class))).thenReturn(apiNewsDto);
        when(service.saveNewsInDb(any(NewsDto.class))).thenReturn(existingNewsDto);
        when(service.findNewsInDb(eq(publishedAtDate))).thenReturn(Optional.of(existingNewsDto));
        when(service.updateNewsInDb(eq(publishedAtDate), any(NewsDto.class))).thenReturn(Optional.of(updatedNewsDto));

        List<NewsDto> result = newsService.getTopHeadlines();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(publishedAtDate, result.getFirst().getPublishedAt());
        assertEquals(updatedArticles.size(), result.get(1).getTotalResults());
        assertEquals("ok", result.get(1).getStatus());

        verify(service).updateNewsInDb(eq(publishedAtDate), argThat(news ->
                        news.getArticles().size() == updatedArticles.size() &&
                        news.getStatus().equals("ok")
        ));
    }

    @Test
    void testGetAllData_Success() throws Exception {
        NewsDto news1 = NewsDto.builder().totalResults(1).publishedAt("2023-10-10").articles(List.of(NewsDto.Article.builder().title("News 1").build())).build();

        NewsDto news2 = NewsDto.builder().totalResults(1).publishedAt("2023-10-11").articles(List.of(NewsDto.Article.builder().title("News 2").build())).build();

        List<NewsDto> expectedNewsList = Arrays.asList(news1, news2);
        when(service.getAllNewsFromDb()).thenReturn(expectedNewsList);

        List<NewsDto> actualNewsList = newsService.getAllData();
        assertEquals(expectedNewsList, actualNewsList);
        verify(service).getAllNewsFromDb();
    }

    @Test
    void testJsonProcessingExceptionThrown() throws JsonProcessingException {
        String response = "{\"totalResults\": 1, \"articles\": [{\"publishedAt\": \"2024-10-29T12:00:00Z\"}]}";
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(response);

        when(objectMapper.readValue(response, NewsDto.class)).thenThrow(new JsonProcessingException("Failed to parse JSON") {
        });

        NewsRetrievalException exception = assertThrows(NewsRetrievalException.class, () -> {
            newsService.getTopHeadlines();
        });

        assertEquals("Failed to parse news data from API response.", exception.getMessage());
        verify(service, never()).saveNewsInDb(any());
    }
}
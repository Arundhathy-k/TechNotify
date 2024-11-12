package com.kovan;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kovan.app.service.NewsService;
import com.kovan.dto.NewsDto;
import com.kovan.entity.NewsEntity;
import com.kovan.exception.NewsRetrievalException;
import com.kovan.mapper.NewsMapper;
import com.kovan.repository.NewsRepository;
import com.kovan.service.NewsRepositoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
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
    private NewsRepositoryService newsRepositoryService;

    @Mock
    private NewsRepository newsRepository;

    @Mock
    private NewsMapper newsMapper;

    @InjectMocks
    private NewsService newsService;

    LocalDate today = LocalDate.now();
    LocalDate yesterday = today.minusDays(1);

    @Test
    void testGetTopHeadlines_Success() throws Exception {

        String apiResponse = "{ \"articles\": ["
                + "{ \"title\": \"Today's News\", \"publishedAt\": \"" + today + "\" },"
                + "{ \"title\": \"Yesterday's News\", \"publishedAt\": \"" + yesterday + "\" }"
                + "], \"totalResults\": 2 }";

        NewsDto.Article todaysArticle = NewsDto.Article.builder()
                .title("Today's News")
                .publishedAt(today.toString())
                .build();

        NewsDto.Article yesterdaysArticle = NewsDto.Article.builder()
                .title("Yesterday's News")
                .publishedAt(yesterday.toString())
                .build();

        NewsDto apiNewsDto = NewsDto.builder()
                .totalResults(2)
                .articles(List.of(todaysArticle, yesterdaysArticle))
                .build();

        NewsEntity yesterdayNewsEntity = NewsEntity.builder()
                .publishedAt(yesterday.toString())
                .totalResults(1)
                .articles(Collections.emptyList())
                .build();

        NewsEntity todayNewsEntity = NewsEntity.builder()
                .publishedAt(today.toString())
                .totalResults(1)
                .articles(Collections.emptyList())
                .build();

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(apiResponse);
        when(objectMapper.readValue(anyString(), eq(NewsDto.class))).thenReturn(apiNewsDto);
        when(newsRepository.findByPublishedAt(yesterday.toString())).thenReturn(Optional.empty());
        when(newsRepository.findByPublishedAt(today.toString())).thenReturn(Optional.empty());
        when(newsRepository.findByPublishedAtIn(List.of(yesterday.toString(), today.toString())))
                .thenReturn(List.of(yesterdayNewsEntity, todayNewsEntity));
        when(newsMapper.toDto(yesterdayNewsEntity)).thenReturn(NewsDto.builder().publishedAt(yesterday.toString()).build());
        when(newsMapper.toDto(todayNewsEntity)).thenReturn(NewsDto.builder().publishedAt(today.toString()).build());

        List<NewsDto> result = newsService.getTopHeadlines();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(yesterday.toString(), result.get(0).getPublishedAt());
        assertEquals(today.toString(), result.get(1).getPublishedAt());

    }

    @Test
    void testGetTopHeadlines_NoArticlesFetched_ShouldReturnEmpty() throws Exception {
        String apiResponse = "{ \"articles\": [], \"totalResults\": 0 }";

        NewsDto emptyApiNewsDto = NewsDto.builder()
                .totalResults(0)
                .articles(Collections.emptyList())
                .build();

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(apiResponse);
        when(objectMapper.readValue(anyString(), eq(NewsDto.class))).thenReturn(emptyApiNewsDto);
        when(newsRepository.findByPublishedAt(yesterday.toString())).thenReturn(Optional.empty());
        when(newsRepository.findByPublishedAt(today.toString())).thenReturn(Optional.empty());
        when(newsRepository.findByPublishedAtIn(List.of(yesterday.toString(), today.toString())))
                .thenReturn(Collections.emptyList());

        List<NewsDto> result = newsService.getTopHeadlines();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetAllData_Success() throws Exception {
        NewsDto news1 = NewsDto.builder().totalResults(1).publishedAt("2023-10-10").articles(List.of(NewsDto.Article.builder().title("News 1").build())).build();

        NewsDto news2 = NewsDto.builder().totalResults(1).publishedAt("2023-10-11").articles(List.of(NewsDto.Article.builder().title("News 2").build())).build();

        List<NewsDto> expectedNewsList = Arrays.asList(news1, news2);
        when(newsRepositoryService.getAllNewsFromDb()).thenReturn(expectedNewsList);

        List<NewsDto> actualNewsList = newsService.getAllData();
        assertEquals(expectedNewsList, actualNewsList);
        verify(newsRepositoryService).getAllNewsFromDb();
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
        verify(newsRepository, never()).findByPublishedAt(anyString());
        verify(newsRepositoryService, never()).saveNewsInDb(any());
    }
}
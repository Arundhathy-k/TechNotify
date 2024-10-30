package com.kovan.unit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kovan.app.service.NewsService;
import com.kovan.dto.NewsDto;
import com.kovan.exception.NewsRetrievalException;
import com.kovan.repository.NewsRepository;
import com.kovan.service.NewsRepositoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class NewsServiceTests {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private NewsRepositoryService newsRepositoryService;

    @Mock
    private NewsRepository newsRepository;

    @InjectMocks
    private NewsService newsService;

    @Test
    void testGetTopHeadlines_Success() throws Exception {
        NewsDto newsDto = NewsDto.builder().totalResults(1).publishedAt("2023-10-10").articles(List.of(NewsDto.Article.builder().title("Sample News").publishedAt("2023-10-10T10:10:10Z").build())).build();

        String response = "{ \"articles\": [{ \"title\": \"Sample News\", \"publishedAt\": \"2023-10-10T10:10:10Z\" }], \"totalResults\": 1 }";

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(response);
        when(objectMapper.readValue(anyString(), eq(NewsDto.class))).thenReturn(newsDto);

        when(newsRepository.findByPublishedAt("2023-10-10")).thenReturn(Optional.empty());
        when(newsRepositoryService.saveNewsInDb(newsDto)).thenReturn(newsDto);

        NewsDto result = newsService.getTopHeadlines();

        assertNotNull(result);
        assertEquals(1, result.getTotalResults());
        assertEquals("Sample News", result.getArticles().getFirst().getTitle());
        assertEquals("2023-10-10T10:10:10Z", result.getArticles().getFirst().getPublishedAt());

        verify(restTemplate).getForObject(anyString(), eq(String.class));
        verify(objectMapper).readValue(response, NewsDto.class);
        verify(newsRepository).findByPublishedAt("2023-10-10");
        verify(newsRepositoryService).saveNewsInDb(newsDto);
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
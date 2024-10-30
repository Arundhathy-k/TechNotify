package com.kovan.integration;


import com.kovan.app.service.NewsService;
import com.kovan.dto.NewsDto;
import com.kovan.repository.NewsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import java.util.List;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
public class NewsServiceControllerIT {

    @Autowired
    private NewsService newsService;
    @Autowired
    private NewsRepository newsRepository;

    @Test
    public void testFetchAndSaveTopHeadlines() {
        NewsDto newsDto = newsService.getTopHeadlines();
        cleanup();

        assertThat(newsDto).isNotNull();
        assertThat(newsDto.getTotalResults()).isGreaterThan(0);
        assertThat(newsDto.getArticles()).isNotNull();
        assertThat(newsDto.getArticles().size()).isGreaterThan(0);

        List<NewsDto> list = newsService.getAllData();
        assertThat(list.size()).isEqualTo(0);
    }

    @Test
    public void testFetchAllNewsFromDB() {
        newsService.getTopHeadlines();
        List<NewsDto> newsList = newsService.getAllData();

        assertThat(newsList).isNotNull();
        assertThat(newsList.size()).isGreaterThan(0);
        cleanup();

        List<NewsDto> list = newsService.getAllData();
        assertThat(list.size()).isEqualTo(0);

    }

    private void cleanup() {
        newsRepository.deleteAll();
    }

}
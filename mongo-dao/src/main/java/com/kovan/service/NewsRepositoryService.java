package com.kovan.service;

import com.kovan.dto.NewsDto;
import com.kovan.entity.NewsEntity;
import com.kovan.exception.NewsRetrievalException;
import com.kovan.mapper.NewsMapper;
import com.kovan.repository.NewsRepository;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import static java.util.Objects.nonNull;
import static java.util.Optional.of;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Service
public class NewsRepositoryService {

    private final NewsRepository newsRepository;
    private final NewsMapper newsMapper;
    private final CacheManager cacheManager;

    public NewsRepositoryService(NewsRepository newsRepository, NewsMapper newsMapper, CacheManager cacheManager) {
        this.newsRepository = newsRepository;
        this.newsMapper = newsMapper;
        this.cacheManager = cacheManager;
    }

    public NewsDto saveNewsInDb(NewsDto newsDto) {
        if (isNull(newsDto)) {
            throw new NewsRetrievalException("NewsDto cannot be null");
        }

        NewsEntity newsEntity = newsMapper.toEntity(newsDto);
        NewsEntity savedNews = newsRepository.save(newsEntity);

        Cache cache = cacheManager.getCache("news");
        if (nonNull(cache)) {
            cache.put(savedNews.getPublishedAt(), newsMapper.toDto(savedNews));
        }

        System.out.println("News saved in db .........");
        return newsMapper.toDto(savedNews);
    }

    @Cacheable(value = "news", key = "#date")
    public Optional<NewsDto> findNewsInDb(String date) {
        System.out.println("News fetched from db.........");
        return newsRepository.findByPublishedAt(date)
                .map(newsMapper::toDto);
    }

    public Optional<NewsDto> updateNewsInDb(String publishedAt, NewsDto updatedNewsDto) {

        if (isBlank(publishedAt) || isNull(updatedNewsDto)) {
            throw new NewsRetrievalException("Published date and NewsDto cannot be null");
        }

        NewsEntity existingNewsEntity = newsRepository.findByPublishedAt(publishedAt)
                .orElseThrow(() -> new NewsRetrievalException("News with published date " + publishedAt + " not found"));

        NewsEntity newNewsEntity = newsMapper.toEntity(updatedNewsDto);

        existingNewsEntity.setStatus(newNewsEntity.getStatus());
        existingNewsEntity.setArticles(newNewsEntity.getArticles());
        existingNewsEntity.setTotalResults(newNewsEntity.getTotalResults());

        NewsEntity savedEntity = newsRepository.save(existingNewsEntity);

        Cache cache = cacheManager.getCache("news");
        if (nonNull(cache)) {
            cache.put(publishedAt, newsMapper.toDto(savedEntity));
        }

        System.out.println("News updated in db .........");
        return of(newsMapper.toDto(savedEntity));
    }

    @Cacheable(value = "allNews")
    public List<NewsDto> getAllNewsFromDb() {
        List<NewsEntity> newsEntities = newsRepository.findAll();
        System.out.println("Fetched all news from db.........");
        return newsEntities.stream()
                .map(newsMapper::toDto)
                .toList();
    }

    @CacheEvict(value = "news", allEntries = true)
    public void deleteAllFromDb() {
        System.out.println("News deleted from db.........");
        newsRepository.deleteAll();
    }
}
package com.kovan.service;

import com.kovan.dto.NewsDto;
import com.kovan.entity.NewsEntity;
import com.kovan.exception.NewsRetrievalException;
import com.kovan.mapper.NewsMapper;
import com.kovan.repository.NewsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import static java.util.Objects.nonNull;
import static java.util.Optional.of;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Service
@Slf4j
public class NewsRepositoryService {

    private final NewsRepository newsRepository;
    private final NewsMapper newsMapper;
    private final CacheManager cacheManager;

    public NewsRepositoryService(NewsRepository newsRepository, NewsMapper newsMapper, CacheManager cacheManager) {
        this.newsRepository = newsRepository;
        this.newsMapper = newsMapper;
        this.cacheManager = cacheManager;
    }

    @Transactional
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

        log.info("News saved in db .........");
        return newsMapper.toDto(savedNews);
    }

    @Cacheable(value = "news", key = "#date")
    public Optional<NewsDto> findNewsInDb(String date) {

        Optional<NewsDto> news = newsRepository.findByPublishedAt(date).map(newsMapper::toDto);
        if (news.isEmpty()) {
            log.warn("No news found for date: {}", date);
        }

        log.info("News fetched from db.........");
        return news;
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

        log.info("News updated in db .........");
        return of(newsMapper.toDto(savedEntity));
    }

    @Cacheable(value = "allNews")
    public List<NewsDto> getAllNewsFromDb() {
        List<NewsEntity> newsEntities = newsRepository.findAll();
        log.info("Fetched all news from db.........");
        return newsEntities.stream()
                .map(newsMapper::toDto)
                .toList();
    }

    @CacheEvict(value = { "news", "allNews" }, allEntries = true)
    public void deleteAllFromDb() {
        log.info("News deleted from db.........");
        newsRepository.deleteAll();
    }
}
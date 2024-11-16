package com.kovan.service;

import com.kovan.dto.NewsDto;
import com.kovan.entity.NewsEntity;
import com.kovan.exception.NewsRetrievalException;
import com.kovan.mapper.NewsMapper;
import com.kovan.repository.NewsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import static java.util.Optional.of;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Service
public class NewsRepositoryService {

    @Autowired
    private NewsRepository newsRepository;

    @Autowired
    private NewsMapper newsMapper;

    public NewsDto saveNewsInDb(NewsDto newsDto) {
        if (isNull(newsDto)) {
            throw new NewsRetrievalException("NewsDto cannot be null");
        }

        NewsEntity newsEntity = newsMapper.toEntity(newsDto);
        NewsEntity savedNews = newsRepository.save(newsEntity);
        return newsMapper.toDto(savedNews);
    }
    public Optional<NewsDto> findNewsInDb(String date) {

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

        return of(newsMapper.toDto(newsRepository.save(existingNewsEntity)));
    }

    public List<NewsDto> getAllNewsFromDb() {

        List<NewsEntity> newsEntities = newsRepository.findAll();

        return newsEntities.stream()
                .map(newsMapper::toDto)
                .collect(toList());
    }
}

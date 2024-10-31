package com.kovan.service;

import com.kovan.dto.NewsDto;
import com.kovan.entity.NewsEntity;
import com.kovan.exception.NewsRetrievalException;
import com.kovan.mapper.NewsMapper;
import com.kovan.repository.NewsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class NewsRepositoryService {

    @Autowired
    private NewsRepository newsRepository;

    @Autowired
    private NewsMapper newsMapper;

    public NewsDto saveNewsInDb(NewsDto newsDto) {
        if (Objects.isNull(newsDto)) {
            throw new NewsRetrievalException("NewsDto cannot be null");
        }

        NewsEntity newsEntity = newsMapper.toEntity(newsDto);
        NewsEntity savedNews = newsRepository.save(newsEntity);

        return newsMapper.toDto(savedNews);
    }

    public List<NewsDto> getAllNewsFromDb() {
        List<NewsEntity> newsEntities = newsRepository.findAll();

        return newsEntities.stream()
                .map(newsMapper::toDto)
                .collect(Collectors.toList());
    }
}

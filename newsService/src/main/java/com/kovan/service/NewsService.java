package com.kovan.service;

import com.kovan.dto.NewsDto;
import com.kovan.mapper.NewsMapper;
import com.kovan.model.NewsEntity;
import com.kovan.repository.NewsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NewsService {

    @Autowired
    private NewsRepository newsRepository;

    @Autowired
    private NewsMapper newsMapper;


    public NewsDto addNews(NewsDto newsDto) {

        NewsEntity newsEntity = newsMapper.toEntity(newsDto);
        NewsEntity savedNews = newsRepository.save(newsEntity);

        return newsMapper.toDto(savedNews);
    }

    public List<NewsDto> getAllNews() {
        List<NewsEntity> newsEntities = newsRepository.findAll();
        return newsEntities.stream()
                .map(newsMapper::toDto)
                .collect(Collectors.toList());

    }
}

package com.kovan.mapper;

import com.kovan.dto.NewsDto;
import com.kovan.model.NewsEntity;
import org.springframework.stereotype.Component;

@Component
public class NewsMapper {

    public NewsDto toDto(NewsEntity newsEntity) {
        if (newsEntity == null) {
            return null;
        }
        NewsDto newsDto = new NewsDto();
        newsDto.setId(newsEntity.getId());
        newsDto.setDescription(newsEntity.getDescription());

        return newsDto;
    }

    public NewsEntity toEntity(NewsDto newsDto) {
        if (newsDto == null) {
            return null;
        }

        NewsEntity newsEntity = new NewsEntity();
        newsEntity.setId(newsDto.getId());
        newsEntity.setDescription(newsDto.getDescription());

        return newsEntity;
    }
}

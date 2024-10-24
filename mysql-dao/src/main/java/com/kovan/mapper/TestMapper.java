package com.kovan.mapper;

import com.kovan.dto.TestDto;
import com.kovan.entity.TestEntity;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class TestMapper {


    public TestDto toDto(TestEntity testEntity) {
        if (Objects.isNull(testEntity)) {
            return null;
        }
        return TestDto.builder()
                .id(testEntity.getId())
                .description(testEntity.getDescription())
                .createdBy(testEntity.getCreatedBy())
                .createdOn(testEntity.getCreatedOn())
                .updatedOn(testEntity.getUpdatedOn())
                .updatedBy(testEntity.getUpdatedBy())
                .build();
    }

    public TestEntity toEntity(TestDto testDto) {
        if (Objects.isNull(testDto)) {
            return null;
        }
        return TestEntity.builder()
                .id(testDto.getId())
                .description(testDto.getDescription())
                .createdBy(testDto.getCreatedBy())
                .createdOn(testDto.getCreatedOn())
                .updatedOn(testDto.getUpdatedOn())
                .updatedBy(testDto.getUpdatedBy())
                .build();
    }
}

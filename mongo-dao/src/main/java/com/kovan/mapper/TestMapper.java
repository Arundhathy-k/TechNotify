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
                .build();
    }

    public TestEntity toEntity(TestDto testDto) {
        if (Objects.isNull(testDto)) {
            return null;
        }
        return TestEntity.builder()
                .id(testDto.getId())
                .description(testDto.getDescription())
                .build();
    }
}

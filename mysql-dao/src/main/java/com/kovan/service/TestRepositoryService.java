package com.kovan.service;

import com.kovan.dto.TestDto;
import com.kovan.mapper.TestMapper;
import com.kovan.entity.TestEntity;
import com.kovan.repository.TestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TestRepositoryService {

    private final TestRepository testRepository;
    private final TestMapper testMapper;

    @Autowired
    public TestRepositoryService(TestRepository testRepository, TestMapper testMapper) {
        this.testRepository = testRepository;
        this.testMapper = testMapper;
    }


    public TestDto saveTestDataInDb(TestDto testDto) {

        TestEntity testEntity = TestEntity.builder()
                .id(testDto.getId())
                .description(testDto.getDescription())
                .createdBy(testDto.getCreatedBy())
                .createdDate(LocalDate.now())
                .updatedBy(testDto.getUpdatedBy())
                .updatedDate(LocalDate.now())
                .build();

        TestEntity savedTest = testRepository.save(testEntity);

        return testMapper.toDto(savedTest);
    }

    public List<TestDto> getAllTestDataFromDb() {
        List<TestEntity> testEntities;
       testEntities = testRepository.findAll();
        return testEntities.stream()
                .map(testMapper::toDto)
                .collect(Collectors.toList());

    }
}

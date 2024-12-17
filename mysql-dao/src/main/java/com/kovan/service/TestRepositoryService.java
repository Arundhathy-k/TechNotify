package com.kovan.service;

import com.kovan.dto.TestDto;
import com.kovan.mapper.TestMapper;
import com.kovan.entity.TestEntity;
import com.kovan.repository.TestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

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
                .fileName(testDto.getFileName())
                .createdBy(testDto.getCreatedBy())
                .createdDate(testDto.getCreatedDate())
                .updatedBy(testDto.getUpdatedBy())
                .updatedDate(testDto.getUpdatedDate())
                .build();

        TestEntity savedTest = testRepository.save(testEntity);
        return testMapper.toDto(savedTest);
    }

    public List<TestDto> getAllTestDataFromDb() {
        List<TestEntity> testEntities = testRepository.findAll();
        return testEntities.stream()
                .map(testMapper::toDto)
                .toList();
    }

    public Optional<TestDto> findByFileName(String fileName) {
        return testRepository.findByFileName(fileName)
                .map(testMapper::toDto);
    }
    public Optional<TestDto> findDataById(String id){
        return testRepository.findById(id).map(testMapper::toDto);
    }
}

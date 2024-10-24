package com.kovan.service;

import com.kovan.dto.TestDto;
import com.kovan.mapper.TestMapper;
import com.kovan.entity.TestEntity;
import com.kovan.repository.TestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TestRepositoryService {

    @Autowired
    private TestRepository testRepository;

    @Autowired
    private TestMapper testMapper;


    public TestDto saveTestDataInDb(TestDto testDto) {

        TestEntity testEntity = testMapper.toEntity(testDto);
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

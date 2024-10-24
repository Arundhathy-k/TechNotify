package com.kovan.app.service;


import com.kovan.api.model.TestRequest;
import com.kovan.dto.TestDto;
import com.kovan.service.TestRepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;


@Service
public class TestService {


    private final TestRepositoryService service;

    @Autowired
    public TestService(TestRepositoryService service) {
        this.service = service;
    }


    public TestDto addData(TestRequest testRequest) {

        if (Objects.isNull(testRequest)) {
            return null;
        }
        TestDto testDto = TestDto.builder()
                .id(testRequest.getId())
                .description(testRequest.getDescription())
                .createdBy(testRequest.getCreatedBy())
                .updatedBy(testRequest.getUpdatedBy())
                .build();
        return service.saveTestDataInDb(testDto);

    }

    public List<TestDto> getAllData() {

     return service.getAllTestDataFromDb();

    }
}
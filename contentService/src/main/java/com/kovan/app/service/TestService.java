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


    @Autowired
    private TestRepositoryService service;


    public TestDto addData(TestRequest testRequest) {

        if (Objects.isNull(testRequest)) {
            return null;
        }
        TestDto testDto = TestDto.builder()
                .id(testRequest.getId())
                .description(testRequest.getDescription())
                .createdBy(testRequest.getCreatedBy())
                .createdOn(LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")))
                .updatedOn(LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")))
                .updatedBy(testRequest.getUpdatedBy())
                .build();
        return service.saveTestDataInDb(testDto);

    }

    public List<TestDto> getAllData() {

     return service.getAllTestDataFromDb();

    }
}
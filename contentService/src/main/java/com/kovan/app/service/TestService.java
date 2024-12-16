package com.kovan.app.service;

import com.kovan.api.model.TestRequest;
import com.kovan.dto.TestDto;
import com.kovan.service.TestRepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class TestService {

    private final TestRepositoryService service;

    @Autowired
    public TestService(TestRepositoryService service) {
        this.service = service;
    }

    public TestDto addOrUpdateData(TestRequest testRequest) {

        Optional<TestDto> existingData = service.findByFileName(testRequest.getFileName());

        if (existingData.isPresent()) {
            TestDto existingTestDto = existingData.get();
            existingTestDto.setUpdatedBy(testRequest.getUpdatedBy());
            return service.saveTestDataInDb(existingTestDto);
        } else {
            TestDto newTestDto = TestDto.builder()
                    .id(testRequest.getId())
                    .fileName(testRequest.getFileName())
                    .createdBy(testRequest.getCreatedBy())
                    .updatedBy(testRequest.getUpdatedBy())
                    .createdDate(Instant.now())
                    .updatedDate(Instant.now())
                    .build();
            return service.saveTestDataInDb(newTestDto);
        }
    }

    public List<TestDto> getAllData() {
        return service.getAllTestDataFromDb();
    }
}

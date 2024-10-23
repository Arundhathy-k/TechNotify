package com.kovan.app.service;


import com.kovan.api.model.TestRequest;
import com.kovan.dto.TestDto;
import com.kovan.service.TestRepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;



@Service
public class TestService {


    @Autowired
    private TestRepositoryService service;


    public TestDto addData(TestRequest testRequest) {

        if (testRequest == null) {
            return null;
        }
       TestDto testDto = TestDto.builder()
                .id(testRequest.getId())
                .description(testRequest.getDescription())
                .build();
      return  service.saveTestDataInDb(testDto);

    }

    public List<TestDto> getAllData() {

     return service.getAllTestDataFromDb();

    }
}
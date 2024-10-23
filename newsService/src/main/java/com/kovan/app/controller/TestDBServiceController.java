package com.kovan.app.controller;

import com.kovan.api.model.TestRequest;
import com.kovan.dto.TestDto;
import com.kovan.app.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/data")
public class TestDBServiceController {

    @Autowired
    private TestService testService;


    @PostMapping("/add")
    public ResponseEntity<TestDto> addTestData(@RequestBody TestRequest testRequest) {
        TestDto createdTestData = testService.addData(testRequest);
        return new ResponseEntity<>(createdTestData, HttpStatus.CREATED);
    }

    @GetMapping("/fetch")
    public ResponseEntity<List<TestDto>> getAllTestData() {
        List<TestDto> testlist = testService.getAllData();
        return new ResponseEntity<>(testlist, HttpStatus.OK);
    }

}

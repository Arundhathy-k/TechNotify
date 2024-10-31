package com.kovan.app.controller;

import com.kovan.app.service.NewsService;
import com.kovan.dto.NewsDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/news")
public class NewsServiceController {

    @Autowired
    private NewsService newsService;

    @PostMapping("/top-headlines")
    public ResponseEntity<NewsDto> fetchAndSaveTopHeadlines() {

        return new ResponseEntity<>(newsService.getTopHeadlines(), HttpStatus.OK);
    }

    @GetMapping("/fetchAllNews")
    public ResponseEntity<List<NewsDto>> fetchAllNewsFromDB() {
        return new ResponseEntity<>(newsService.getAllData(), HttpStatus.OK);
    }

}

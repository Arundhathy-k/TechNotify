package com.kovan.app.controller;

import com.kovan.dto.NewsDto;
import com.kovan.app.service.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/news")
public class NewsServiceController {

    @Autowired
    private NewsService newsService;


    @GetMapping("/top-headlines")
    public ResponseEntity<NewsDto> fetchAndSaveTopHeadlines() {

        return new ResponseEntity<>(newsService.getTopHeadlines(), HttpStatus.OK);
    }

}

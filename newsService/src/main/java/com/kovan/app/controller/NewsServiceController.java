package com.kovan.app.controller;

import com.kovan.app.service.NewsService;
import com.kovan.dto.NewsDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/news")
public class NewsServiceController {

    private final NewsService newsService;

    public NewsServiceController(NewsService newsService) {
        this.newsService = newsService;
    }

    @PostMapping("/addTechNews")
    public ResponseEntity<NewsDto> saveTechNews() {
        return new ResponseEntity<>(newsService.getTopHeadlines(), HttpStatus.OK);
    }

    @GetMapping("/fetchAllTechNews")
    public ResponseEntity<List<NewsDto>> fetchAllTechNewsFromDB(){
        return new ResponseEntity<>(newsService.getAllData(),HttpStatus.OK);
    }
}

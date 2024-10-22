package com.kovan.controller;

import com.kovan.dto.NewsDto;
import com.kovan.service.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    @Autowired
    private NewsService newsService;


    @PostMapping("/add")
    public ResponseEntity<NewsDto> addNews(@RequestBody NewsDto newsDto) {
        NewsDto createdNews = newsService.addNews(newsDto);
        return new ResponseEntity<>(createdNews, HttpStatus.CREATED);
    }

    @GetMapping("/fetch")
    public ResponseEntity<List<NewsDto>> getAllNews() {
        List<NewsDto> newsList = newsService.getAllNews();
        return new ResponseEntity<>(newsList, HttpStatus.OK);
    }

}

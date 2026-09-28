package com.trai.engine.controller;

import com.trai.engine.search.NewsSearchDocument;
import com.trai.engine.search.NewsSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@CrossOrigin(origins = "*")
public class NewsSearchController {

    private final NewsSearchService searchService;

    public NewsSearchController(NewsSearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/news")
    public ResponseEntity<List<NewsSearchDocument>> searchNews(@RequestParam(required = false, defaultValue = "") String q) {
        List<NewsSearchDocument> results = searchService.searchNews(q);
        return ResponseEntity.ok(results);
    }
}

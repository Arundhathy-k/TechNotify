package com.kovan.repository;

import com.kovan.entity.NewsEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NewsRepository extends MongoRepository<NewsEntity, String> {
    NewsEntity findByPublishedAt(String publishedAt);
}

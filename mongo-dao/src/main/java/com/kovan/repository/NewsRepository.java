package com.kovan.repository;

import com.kovan.entity.NewsEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface NewsRepository extends MongoRepository<NewsEntity, String> {
   Optional<NewsEntity> findByPublishedAt(String publishedAt);

}

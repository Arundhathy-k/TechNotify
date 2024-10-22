package com.kovan.repository;

import com.kovan.model.NewsEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface NewsRepository extends MongoRepository<NewsEntity,String> {

}

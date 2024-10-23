package com.kovan.repository;

import com.kovan.entity.TestEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TestRepository extends MongoRepository<TestEntity,String> {

}

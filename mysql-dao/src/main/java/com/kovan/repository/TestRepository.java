package com.kovan.repository;

import com.kovan.entity.TestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TestRepository extends JpaRepository<TestEntity,String> {
    Optional<TestEntity> findByFileName(String fileName);
}

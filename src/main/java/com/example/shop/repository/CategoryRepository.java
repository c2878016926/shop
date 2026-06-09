package com.example.shop.repository;

import com.example.shop.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * 按名称模糊查询分类
     */
    List<Category> findByNameContainingIgnoreCase(String name);

    /**
     * 检查分类名称是否已存在
     */
    boolean existsByName(String name);
}

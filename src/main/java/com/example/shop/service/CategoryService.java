package com.example.shop.service;

import com.example.shop.entity.Category;
import com.example.shop.repository.CategoryRepository;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final JdbcTemplate jdbcTemplate;

    public CategoryService(CategoryRepository categoryRepository, JdbcTemplate jdbcTemplate) {
        this.categoryRepository = categoryRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Category> findAll() {
        return categoryRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    public Category findById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("分类不存在，ID: " + id));
    }

    public List<Category> search(String name) {
        if (name == null || name.trim().isEmpty()) {
            return findAll();
        }
        return categoryRepository.findByNameContainingIgnoreCase(name.trim());
    }

    @Transactional
    public Category save(Category category) {
        return categoryRepository.save(category);
    }

    @Transactional
    public void deleteById(Long id) {
        categoryRepository.deleteById(id);
    }

    /**
     * 使用 JdbcTemplate 原生 SQL 统计每个分类下的商品数量
     * 演示左连接 + 分组统计
     */
    public List<Map<String, Object>> getProductCountByCategory() {
        String sql = "SELECT c.id, c.name, COUNT(p.id) AS product_count, COALESCE(SUM(p.stock), 0) AS total_stock "
                + "FROM categories c LEFT JOIN products p ON c.id = p.category_id "
                + "GROUP BY c.id, c.name ORDER BY product_count DESC";
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * 使用 JdbcTemplate 查询指定分类下的商品价格统计
     */
    public Map<String, Object> getCategoryPriceStats(Long categoryId) {
        String sql = "SELECT COUNT(*) AS product_count, "
                + "COALESCE(MIN(price), 0) AS min_price, "
                + "COALESCE(MAX(price), 0) AS max_price, "
                + "COALESCE(AVG(price), 0) AS avg_price "
                + "FROM products WHERE category_id = ?";
        return jdbcTemplate.queryForMap(sql, categoryId);
    }
}

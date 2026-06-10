package com.example.shop.service;

import com.example.shop.entity.Product;
import com.example.shop.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 商品服务层
 * 提供商品的 CRUD、并发库存扣减、JDBC 原生 SQL 查询等功能
 */
@Service
@Transactional(readOnly = true)
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final JdbcTemplate jdbcTemplate;
    private final EntityManager entityManager;

    public ProductService(ProductRepository productRepository, JdbcTemplate jdbcTemplate, EntityManager entityManager) {
        this.productRepository = productRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.entityManager = entityManager;
    }

    public List<Product> findAll() {
        return productRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    public Page<Product> findPage(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        return productRepository.findAll(pageable);
    }

    public Page<Product> searchPage(String name, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        if (name == null || name.trim().isEmpty()) {
            return productRepository.findAll(pageable);
        }
        return productRepository.findByNameContainingIgnoreCase(name.trim(), pageable);
    }

    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在，ID: " + id));
    }

    public List<Product> search(String name) {
        if (name == null || name.trim().isEmpty()) {
            return productRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        }
        return productRepository.findByNameContainingIgnoreCase(name.trim());
    }

    @Transactional
    public Product save(Product product) {
        log.info("保存商品: id={}, name={}", product.getId(), product.getName());
        return productRepository.save(product);
    }

    /**
     * 使用自定义ID保存商品（JDBC方式绕过JPA的IDENTITY策略）
     * 如果指定的ID已存在，抛出异常
     */
    @Transactional
    public Product saveWithCustomId(Product product) {
        // 检查ID是否已存在
        Long existCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM products WHERE id = ?", Long.class, product.getId());
        if (existCount != null && existCount > 0) {
            throw new IllegalArgumentException("ID " + product.getId() + " 已被占用，请使用其他ID");
        }
        Long categoryId = product.getCategory() != null ? product.getCategory().getId() : null;
        jdbcTemplate.update(
                "INSERT INTO products (id, name, description, price, stock, category_id) VALUES (?, ?, ?, ?, ?, ?)",
                product.getId(), product.getName(), product.getDescription(),
                product.getPrice(), product.getStock(), categoryId);
        // 确保 AUTO_INCREMENT 不会低于已插入的ID
        jdbcTemplate.execute("ALTER TABLE products AUTO_INCREMENT = " + (product.getId() + 1));
        entityManager.flush();
        entityManager.clear();
        return product;
    }

    /**
     * 修改已有商品的ID（JDBC方式直接更新主键）
     */
    @Transactional
    public void updateProductId(Long oldId, Long newId) {
        if (oldId.equals(newId)) return;
        // 检查新ID是否已被占用
        Long existCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM products WHERE id = ?", Long.class, newId);
        if (existCount != null && existCount > 0) {
            throw new IllegalArgumentException("ID " + newId + " 已被占用，请使用其他ID");
        }
        // 直接更新主键（MySQL支持更新自增列）
        jdbcTemplate.update("UPDATE products SET id = ? WHERE id = ?", newId, oldId);
        // 确保 AUTO_INCREMENT 不会低于已插入的ID
        jdbcTemplate.execute("ALTER TABLE products AUTO_INCREMENT = " + (Math.max(oldId, newId) + 1));
        entityManager.flush();
        entityManager.clear();
    }

    /**
     * 检查ID是否已存在
     */
    public boolean existsById(Long id) {
        return productRepository.existsById(id);
    }

    @Transactional
    public void deleteById(Long id) {
        log.info("删除商品: id={}", id);
        // 先删除引用该商品的所有订单（外键约束）
        int deleted = jdbcTemplate.update("DELETE FROM orders WHERE product_id = ?", id);
        productRepository.deleteById(id);
        log.info("商品已删除，关联订单已清理: {} 条", deleted);
        // 如果删除后表为空，重置自增ID从1开始
        resetAutoIncrementIfEmpty();
    }

    /**
     * 清空所有商品（包括关联的订单）并重置自增ID
     */
    @Transactional
    public void deleteAllAndResetId() {
        log.warn("清空所有商品和订单并重置ID");
        // 先删除所有订单（解除外键约束）
        jdbcTemplate.execute("DELETE FROM orders");
        // 再删除所有商品
        productRepository.deleteAllInBatch();
        // 清除 JPA 会话缓存，确保后续查询不受影响
        entityManager.flush();
        entityManager.clear();
        // 重置自增ID计数器
        jdbcTemplate.execute("ALTER TABLE products AUTO_INCREMENT = 1");
        jdbcTemplate.execute("ALTER TABLE orders AUTO_INCREMENT = 1");
    }

    /**
     * 表为空时重置自增ID计数器
     */
    private void resetAutoIncrementIfEmpty() {
        Long count = productRepository.count();
        if (count != null && count == 0) {
            jdbcTemplate.execute("ALTER TABLE products AUTO_INCREMENT = 1");
        }
    }

    /**
     * 并发库存扣减（使用 synchronized 保证线程安全）
     */
    @Transactional
    public synchronized boolean reduceStock(Long productId, int quantity) {
        log.debug("并发扣库存: productId={}, quantity={}", productId, quantity);
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null || product.getStock() < quantity) {
            return false;
        }
        product.setStock(product.getStock() - quantity);
        productRepository.save(product);
        return true;
    }

    /**
     * 使用 JdbcTemplate 执行原生 SQL 查询库存统计
     */
    public Map<String, Object> getStockStatsByJdbc() {
        String sql = "SELECT COUNT(*) as TOTAL_COUNT, COALESCE(SUM(stock), 0) as TOTAL_STOCK, COALESCE(AVG(price), 0) as AVG_PRICE FROM products";
        return jdbcTemplate.queryForMap(sql);
    }

    /**
     * 使用 JdbcTemplate 查询价格区间商品
     */
    public List<Map<String, Object>> findByPriceRange(BigDecimal min, BigDecimal max) {
        String sql = "SELECT id, name, price, stock FROM products WHERE price BETWEEN ? AND ? ORDER BY price DESC";
        return jdbcTemplate.queryForList(sql, min, max);
    }

    /**
     * 使用 JdbcTemplate 多表关联查询：商品+分类信息
     * 演示 LEFT JOIN + 字段映射
     */
    public List<Map<String, Object>> findProductsWithCategory() {
        String sql = "SELECT p.id, p.name, p.price, p.stock, "
                + "COALESCE(c.name, '未分类') AS category_name "
                + "FROM products p LEFT JOIN categories c ON p.category_id = c.id "
                + "ORDER BY p.id DESC";
        return jdbcTemplate.queryForList(sql);
    }
}

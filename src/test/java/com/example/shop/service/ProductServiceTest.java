package com.example.shop.service;

import com.example.shop.entity.Category;
import com.example.shop.entity.Product;
import com.example.shop.repository.CategoryRepository;
import com.example.shop.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = new Product("测试商品", "测试描述", new BigDecimal("99.99"), 100);
        testProduct = productRepository.save(testProduct);
    }

    @Test
    void testFindById() {
        Product found = productService.findById(testProduct.getId());
        assertNotNull(found);
        assertEquals("测试商品", found.getName());
        assertEquals(new BigDecimal("99.99"), found.getPrice());
    }

    @Test
    void testFindByIdNotFound() {
        assertThrows(IllegalArgumentException.class, () -> productService.findById(99999L));
    }

    @Test
    void testSave() {
        Product newProduct = new Product("新商品", "新描述", new BigDecimal("50.00"), 20);
        Product saved = productService.save(newProduct);
        assertNotNull(saved.getId());
        assertEquals("新商品", saved.getName());
    }

    @Test
    void testDeleteById() {
        productService.deleteById(testProduct.getId());
        assertFalse(productRepository.findById(testProduct.getId()).isPresent());
    }

    @Test
    void testReduceStockSuccess() {
        boolean result = productService.reduceStock(testProduct.getId(), 10);
        assertTrue(result);
        Product updated = productRepository.findById(testProduct.getId()).get();
        assertEquals(90, updated.getStock());
    }

    @Test
    void testReduceStockInsufficient() {
        boolean result = productService.reduceStock(testProduct.getId(), 200);
        assertFalse(result);
        Product unchanged = productRepository.findById(testProduct.getId()).get();
        assertEquals(100, unchanged.getStock());
    }

    @Test
    void testReduceStockProductNotFound() {
        boolean result = productService.reduceStock(99999L, 1);
        assertFalse(result);
    }

    @Test
    void testGetStockStatsByJdbc() {
        Map<String, Object> stats = productService.getStockStatsByJdbc();
        assertNotNull(stats);
        assertTrue(((Number) stats.get("TOTAL_COUNT")).longValue() >= 1);
    }

    @Test
    void testFindByPriceRange() {
        List<Map<String, Object>> results = productService.findByPriceRange(
                new BigDecimal("1.00"), new BigDecimal("200.00"));
        assertFalse(results.isEmpty());
    }

    @Test
    void testFindProductsWithCategory() {
        // 给商品设置分类
        Category category = new Category("电子产品", "电子设备分类");
        category = categoryRepository.save(category);
        testProduct.setCategory(category);
        productRepository.save(testProduct);

        List<Map<String, Object>> results = productService.findProductsWithCategory();
        assertFalse(results.isEmpty());
        // 验证结果包含 category_name 列
        assertTrue(results.get(0).containsKey("category_name"));
    }
}

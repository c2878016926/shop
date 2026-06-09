package com.example.shop.service;

import com.example.shop.entity.Category;
import com.example.shop.entity.Order;
import com.example.shop.entity.Product;
import com.example.shop.repository.CategoryRepository;
import com.example.shop.repository.OrderRepository;
import com.example.shop.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class OrderAndCategoryServiceTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EntityManager entityManager;

    private Product testProduct;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testCategory = new Category("测试分类", "测试分类描述");
        testCategory = categoryRepository.save(testCategory);

        testProduct = new Product("订单测试商品", "描述", new BigDecimal("88.88"), 100);
        testProduct.setCategory(testCategory);
        testProduct = productRepository.save(testProduct);
    }

    // ===== CategoryService 测试 =====

    @Test
    void testCategoryFindAll() {
        List<Category> categories = categoryService.findAll();
        assertFalse(categories.isEmpty());
    }

    @Test
    void testCategoryFindById() {
        Category found = categoryService.findById(testCategory.getId());
        assertEquals("测试分类", found.getName());
    }

    @Test
    void testCategorySave() {
        Category newCat = new Category("新分类", "新分类描述");
        Category saved = categoryService.save(newCat);
        assertNotNull(saved.getId());
        assertEquals("新分类", saved.getName());
    }

    @Test
    void testGetProductCountByCategory() {
        List<Map<String, Object>> stats = categoryService.getProductCountByCategory();
        assertFalse(stats.isEmpty());
        // 验证结果包含分类名称和商品数量
        assertTrue(stats.get(0).containsKey("name"));
        assertTrue(stats.get(0).containsKey("product_count"));
    }

    @Test
    void testGetCategoryPriceStats() {
        Map<String, Object> stats = categoryService.getCategoryPriceStats(testCategory.getId());
        assertNotNull(stats);
        assertTrue(((Number) stats.get("product_count")).longValue() >= 1);
    }

    // ===== OrderService 测试 =====

    @Test
    void testPlaceOrder() {
        Order order = orderService.placeOrder(testProduct.getId(), 3);
        assertNotNull(order.getId());
        assertEquals(3, order.getQuantity());
        assertEquals(new BigDecimal("88.88"), order.getUnitPrice());
        assertEquals(new BigDecimal("266.64"), order.getTotalAmount());
        assertEquals("PENDING", order.getStatus());
    }

    @Test
    void testPlaceOrderInsufficientStock() {
        assertThrows(IllegalArgumentException.class,
                () -> orderService.placeOrder(testProduct.getId(), 999));
    }

    @Test
    void testUpdateOrderStatus() {
        Order order = orderService.placeOrder(testProduct.getId(), 1);
        Order updated = orderService.updateStatus(order.getId(), "COMPLETED");
        assertEquals("COMPLETED", updated.getStatus());
    }

    @Test
    void testFindByProductId() {
        orderService.placeOrder(testProduct.getId(), 2);
        List<Order> orders = orderService.findByProductId(testProduct.getId());
        assertFalse(orders.isEmpty());
    }

    @Test
    void testFindByStatus() {
        orderService.placeOrder(testProduct.getId(), 1);
        List<Order> pendingOrders = orderService.findByStatus("PENDING");
        assertFalse(pendingOrders.isEmpty());
    }

    @Test
    void testGetOrderRevenueStats() {
        Order order = orderService.placeOrder(testProduct.getId(), 2);
        orderService.updateStatus(order.getId(), "COMPLETED");
        // JPA 需要刷新才能让 JdbcTemplate 在同一事务中看到更新
        entityManager.flush();
        entityManager.clear();

        Map<String, Object> stats = orderService.getOrderRevenueStats();
        assertNotNull(stats);
        assertTrue(((Number) stats.get("order_count")).longValue() >= 1);
    }

    @Test
    void testGetProductSalesRanking() {
        Order order = orderService.placeOrder(testProduct.getId(), 2);
        orderService.updateStatus(order.getId(), "COMPLETED");

        List<Map<String, Object>> ranking = orderService.getProductSalesRanking();
        assertFalse(ranking.isEmpty());
    }
}

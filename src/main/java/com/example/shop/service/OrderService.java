package com.example.shop.service;

import com.example.shop.entity.Order;
import com.example.shop.entity.Product;
import com.example.shop.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final JdbcTemplate jdbcTemplate;

    public OrderService(OrderRepository orderRepository, ProductService productService, JdbcTemplate jdbcTemplate) {
        this.orderRepository = orderRepository;
        this.productService = productService;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    public Order findById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在，ID: " + id));
    }

    public List<Order> findByProductId(Long productId) {
        return orderRepository.findByProductId(productId);
    }

    public List<Order> findByStatus(String status) {
        return orderRepository.findByStatus(status);
    }

    /**
     * 下单：创建订单并扣减库存（事务保证一致性）
     * 体现并发库存扣减
     */
    @Transactional
    public Order placeOrder(Long productId, Integer quantity) {
        log.info("下单: productId={}, quantity={}", productId, quantity);
        boolean ok = productService.reduceStock(productId, quantity);
        if (!ok) {
            throw new IllegalArgumentException("库存不足或商品不存在，下单失败");
        }
        Product product = productService.findById(productId);
        Order order = new Order(product, quantity);
        Order saved = orderRepository.save(order);
        log.info("订单创建成功: orderId={}, totalAmount={}", saved.getId(), saved.getTotalAmount());
        return saved;
    }

    /**
     * 更新订单状态
     */
    @Transactional
    public Order updateStatus(Long id, String status) {
        Order order = findById(id);
        order.setStatus(status);
        return orderRepository.save(order);
    }

    /**
     * 使用 JdbcTemplate 原生 SQL 统计订单销售额
     * 演示多表关联 + 条件过滤 + 聚合函数
     */
    public Map<String, Object> getOrderRevenueStats() {
        String sql = "SELECT COUNT(*) AS order_count, "
                + "COALESCE(SUM(total_amount), 0) AS total_revenue, "
                + "COALESCE(AVG(total_amount), 0) AS avg_order_amount "
                + "FROM orders WHERE status = 'COMPLETED'";
        return jdbcTemplate.queryForMap(sql);
    }

    /**
     * 使用 JdbcTemplate 查询每个商品的销售额排名
     * 演示多表 JOIN + GROUP BY + ORDER BY
     */
    public List<Map<String, Object>> getProductSalesRanking() {
        String sql = "SELECT p.id AS product_id, p.name AS product_name, "
                + "COUNT(o.id) AS order_count, "
                + "COALESCE(SUM(o.total_amount), 0) AS total_sales "
                + "FROM products p LEFT JOIN orders o ON p.id = o.product_id AND o.status = 'COMPLETED' "
                + "GROUP BY p.id, p.name ORDER BY total_sales DESC";
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * 使用 JdbcTemplate 查询指定时间范围内的订单
     */
    public List<Map<String, Object>> findOrdersByDateRange(String startDate, String endDate) {
        String sql = "SELECT o.id, o.product_id, p.name AS product_name, o.quantity, "
                + "o.unit_price, o.total_amount, o.status, o.create_time "
                + "FROM orders o JOIN products p ON o.product_id = p.id "
                + "WHERE o.create_time BETWEEN ? AND ? ORDER BY o.create_time DESC";
        return jdbcTemplate.queryForList(sql, startDate, endDate);
    }
}

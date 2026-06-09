package com.example.shop.repository;

import com.example.shop.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * 按商品ID查询订单列表
     */
    List<Order> findByProductId(Long productId);

    /**
     * 按订单状态查询
     */
    List<Order> findByStatus(String status);

    /**
     * 按商品ID和状态查询
     */
    List<Order> findByProductIdAndStatus(Long productId, String status);
}

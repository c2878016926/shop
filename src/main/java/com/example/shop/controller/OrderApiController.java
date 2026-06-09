package com.example.shop.controller;

import com.example.shop.dto.ApiResponse;
import com.example.shop.entity.Order;
import com.example.shop.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderApiController {

    private final OrderService orderService;

    public OrderApiController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ApiResponse<List<Order>> list() {
        return ApiResponse.success(orderService.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<Order> getById(@PathVariable Long id) {
        return ApiResponse.success(orderService.findById(id));
    }

    /**
     * 按商品ID查询订单
     */
    @GetMapping("/product/{productId}")
    public ApiResponse<List<Order>> byProduct(@PathVariable Long productId) {
        return ApiResponse.success(orderService.findByProductId(productId));
    }

    /**
     * 按状态查询订单
     */
    @GetMapping("/status/{status}")
    public ApiResponse<List<Order>> byStatus(@PathVariable String status) {
        return ApiResponse.success(orderService.findByStatus(status));
    }

    /**
     * 更新订单状态
     */
    @PutMapping("/{id}/status")
    public ApiResponse<Order> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return ApiResponse.success("状态更新成功", orderService.updateStatus(id, status));
    }

    /**
     * 订单销售额统计（JdbcTemplate 聚合查询）
     */
    @GetMapping("/revenue-stats")
    public ApiResponse<Map<String, Object>> revenueStats() {
        return ApiResponse.success(orderService.getOrderRevenueStats());
    }

    /**
     * 按时间范围查询订单（JdbcTemplate 多表 JOIN）
     */
    @GetMapping("/date-range")
    public ApiResponse<List<Map<String, Object>>> byDateRange(
            @RequestParam String startDate, @RequestParam String endDate) {
        return ApiResponse.success(orderService.findOrdersByDateRange(startDate, endDate));
    }
}

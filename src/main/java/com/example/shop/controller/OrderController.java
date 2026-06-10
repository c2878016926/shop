package com.example.shop.controller;

import com.example.shop.entity.Order;
import com.example.shop.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单管理 Web 控制器
 * 提供订单列表、订单详情、状态更新等页面操作
 */
@Controller
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** 计算订单列表的总销售额并添加到 Model */
    private void addTotalSales(List<Order> orders, Model model) {
        BigDecimal totalSales = orders.stream()
                .filter(o -> o.getTotalAmount() != null)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("totalSales", totalSales);
    }

    @GetMapping("/orders")
    public String orderList(Model model) {
        List<Order> orders = orderService.findAll();
        model.addAttribute("orders", orders);
        addTotalSales(orders, model);
        return "orders";
    }

    @GetMapping("/orders/product/{productId}")
    public String ordersByProduct(@PathVariable Long productId, Model model) {
        List<Order> orders = orderService.findByProductId(productId);
        model.addAttribute("orders", orders);
        model.addAttribute("filterLabel", "商品ID: " + productId);
        addTotalSales(orders, model);
        return "orders";
    }

    @GetMapping("/orders/status/{status}")
    public String ordersByStatus(@PathVariable String status, Model model) {
        List<Order> orders = orderService.findByStatus(status);
        model.addAttribute("orders", orders);
        model.addAttribute("filterLabel", "状态: " + status);
        addTotalSales(orders, model);
        return "orders";
    }

    @PostMapping("/orders/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam String status,
                               RedirectAttributes redirectAttributes) {
        try {
            orderService.updateStatus(id, status);
            redirectAttributes.addFlashAttribute("msg", "订单状态更新成功！");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/orders";
    }
}

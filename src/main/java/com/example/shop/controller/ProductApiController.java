package com.example.shop.controller;

import com.example.shop.dto.ApiResponse;
import com.example.shop.entity.Product;
import com.example.shop.service.ExternalApiService;
import com.example.shop.service.OrderService;
import com.example.shop.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductApiController {

    private final ProductService productService;
    private final ExternalApiService externalApiService;
    private final OrderService orderService;

    public ProductApiController(ProductService productService, ExternalApiService externalApiService, OrderService orderService) {
        this.productService = productService;
        this.externalApiService = externalApiService;
        this.orderService = orderService;
    }

    @GetMapping
    public ApiResponse<Page<Product>> list(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(productService.findPage(page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<Product> getById(@PathVariable Long id) {
        Product product = productService.findById(id);
        return ApiResponse.success(product);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Product> create(@Valid @RequestBody Product product) {
        Product saved = productService.save(product);
        return ApiResponse.success(201, "创建成功", saved);
    }

    @PutMapping("/{id}")
    public ApiResponse<Product> update(@PathVariable Long id, @Valid @RequestBody Product product) {
        productService.findById(id); // 校验存在性，不存在则抛异常
        product.setId(id);
        Product updated = productService.save(product);
        return ApiResponse.success("更新成功", updated);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> delete(@PathVariable Long id) {
        productService.deleteById(id);
        return ApiResponse.success("删除成功", null);
    }

    @GetMapping("/{id}/usd")
    public ApiResponse<BigDecimal> getUsdPrice(@PathVariable Long id) {
        Product product = productService.findById(id);
        if (product.getPrice() == null) {
            return ApiResponse.error(400, "商品价格无效");
        }
        BigDecimal usd = externalApiService.convertToUsd(product.getPrice());
        return ApiResponse.success(usd);
    }

    /**
     * 下单接口：体现并发库存扣减 + 事务一致性
     */
    @PostMapping("/{id}/order")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Map<String, Object>> placeOrder(@PathVariable Long id,
                                                        @RequestParam(defaultValue = "1") Integer quantity) {
        if (quantity < 1) {
            return ApiResponse.error(400, "购买数量不能小于1");
        }
        var order = orderService.placeOrder(id, quantity);
        return ApiResponse.success(201, "下单成功", Map.of(
                "orderId", order.getId(),
                "productId", id,
                "quantity", quantity,
                "totalAmount", order.getTotalAmount(),
                "status", order.getStatus()
        ));
    }

    /**
     * 商品销售排名（JdbcTemplate 多表 JOIN）
     */
    @GetMapping("/sales-ranking")
    public ApiResponse<List<Map<String, Object>>> salesRanking() {
        return ApiResponse.success(orderService.getProductSalesRanking());
    }

    /**
     * 商品+分类关联查询（JdbcTemplate LEFT JOIN）
     */
    @GetMapping("/with-category")
    public ApiResponse<List<Map<String, Object>>> withCategory() {
        return ApiResponse.success(productService.findProductsWithCategory());
    }
}

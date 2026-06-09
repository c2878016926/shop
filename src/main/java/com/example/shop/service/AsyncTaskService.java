package com.example.shop.service;

import com.example.shop.dto.ProductStats;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.CompletableFuture;

@Service
public class AsyncTaskService {

    private final ProductService productService;

    public AsyncTaskService(ProductService productService) {
        this.productService = productService;
    }

    @Async("taskExecutor")
    public CompletableFuture<ProductStats> calculateStatsAsync() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        var products = productService.findAll();
        long count = products.size();
        BigDecimal totalPrice = products.stream()
                .map(p -> p.getPrice() != null ? p.getPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgPrice = count > 0
                ? totalPrice.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        int totalStock = products.stream().mapToInt(p -> p.getStock() != null ? p.getStock() : 0).sum();

        ProductStats stats = new ProductStats(count, totalPrice, avgPrice, totalStock);
        return CompletableFuture.completedFuture(stats);
    }
}

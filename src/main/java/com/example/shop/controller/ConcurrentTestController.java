package com.example.shop.controller;

import com.example.shop.dto.ConcurrentResult;
import com.example.shop.entity.Product;
import com.example.shop.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

@Controller
public class ConcurrentTestController {

    private final ProductService productService;
    private final Executor taskExecutor;

    public ConcurrentTestController(ProductService productService, Executor taskExecutor) {
        this.productService = productService;
        this.taskExecutor = taskExecutor;
    }

    @GetMapping("/concurrent")
    public String concurrentPage(Model model) {
        model.addAttribute("result", null);
        return "concurrent";
    }

    @PostMapping("/concurrent/test")
    @ResponseBody
    public ConcurrentResult runConcurrentTest(@RequestParam Long productId,
                                               @RequestParam(defaultValue = "10") int threadCount,
                                               @RequestParam(defaultValue = "1") int quantity) {
        Product product;
        try {
            product = productService.findById(productId);
        } catch (IllegalArgumentException e) {
            ConcurrentResult result = new ConcurrentResult();
            result.setSuccessCount(0);
            result.setFailCount(threadCount);
            return result;
        }

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(threadCount);

        long start = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            taskExecutor.execute(() -> {
                try {
                    boolean ok = productService.reduceStock(productId, quantity);
                    if (ok) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long elapsed = System.currentTimeMillis() - start;
        int remainingStock = 0;
        try {
            Product updated = productService.findById(productId);
            remainingStock = updated.getStock() != null ? updated.getStock() : 0;
        } catch (IllegalArgumentException ignored) {}

        ConcurrentResult result = new ConcurrentResult();
        result.setThreadCount(threadCount);
        result.setQuantityPerThread(quantity);
        result.setSuccessCount(successCount.get());
        result.setFailCount(failCount.get());
        result.setRemainingStock(remainingStock);
        result.setElapsedMillis(elapsed);
        return result;
    }
}

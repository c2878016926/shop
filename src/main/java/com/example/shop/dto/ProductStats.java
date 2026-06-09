package com.example.shop.dto;

import java.math.BigDecimal;

public class ProductStats {
    private long totalCount;
    private BigDecimal totalPrice;
    private BigDecimal avgPrice;
    private int totalStock;

    public ProductStats() {}

    public ProductStats(long totalCount, BigDecimal totalPrice, BigDecimal avgPrice, int totalStock) {
        this.totalCount = totalCount;
        this.totalPrice = totalPrice;
        this.avgPrice = avgPrice;
        this.totalStock = totalStock;
    }

    public long getTotalCount() { return totalCount; }
    public void setTotalCount(long totalCount) { this.totalCount = totalCount; }

    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }

    public BigDecimal getAvgPrice() { return avgPrice; }
    public void setAvgPrice(BigDecimal avgPrice) { this.avgPrice = avgPrice; }

    public int getTotalStock() { return totalStock; }
    public void setTotalStock(int totalStock) { this.totalStock = totalStock; }
}

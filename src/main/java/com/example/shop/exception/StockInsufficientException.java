package com.example.shop.exception;

/**
 * 库存不足异常
 * 当库存扣减操作因库存不足而失败时抛出
 */
public class StockInsufficientException extends RuntimeException {

    private final Long productId;
    private final int requested;
    private final int available;

    public StockInsufficientException(Long productId, int requested, int available) {
        super(String.format("库存不足: 商品ID=%d, 请求数量=%d, 当前库存=%d", productId, requested, available));
        this.productId = productId;
        this.requested = requested;
        this.available = available;
    }

    public StockInsufficientException(String message) {
        super(message);
        this.productId = null;
        this.requested = 0;
        this.available = 0;
    }

    public Long getProductId() {
        return productId;
    }

    public int getRequested() {
        return requested;
    }

    public int getAvailable() {
        return available;
    }
}
